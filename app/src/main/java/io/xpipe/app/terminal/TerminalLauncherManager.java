package io.xpipe.app.terminal;

import io.xpipe.app.core.AppNames;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.process.*;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.core.FilePath;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class TerminalLauncherManager {

    private static final SequencedMap<UUID, TerminalLaunchRequest> entries = new LinkedHashMap<>();

    public static void init() {
        TerminalView.get().addListener(new TerminalView.Listener() {
            @Override
            public void onSessionClosed(TerminalView.ShellSession session) {
                var affectedEntry = entries.values().stream()
                        .filter(terminalLaunchRequest -> {
                            return terminalLaunchRequest.getRequest().equals(session.getRequest());
                        })
                        .findFirst();
                if (affectedEntry.isEmpty()) {
                    return;
                }

                affectedEntry.get().abort();
            }
        });
    }

    public static CountDownLatch submitAsync(
            UUID request, ProcessControl processControl, TerminalInitScriptConfig config, FilePath directory) {
        synchronized (entries) {
            var req = entries.get(request);
            if (req == null) {
                req = new TerminalLaunchRequest(request, processControl, config, directory, -1, null, false, null);
                entries.put(request, req);
            } else {
                req.setResult(null);
            }

            req.setupRequestAsync();
            return req.getLatch();
        }
    }

    public static Path sshLaunchExchange() throws BeaconClientException, BeaconServerException {
        TerminalLaunchRequest last;
        synchronized (entries) {
            var all = entries.values().stream().toList();
            last = !all.isEmpty() ? all.getLast() : null;
            if (last == null) {
                throw new BeaconClientException("Unknown launch request");
            }
        }
        return last.waitForCompletion();
    }

    @SuppressWarnings("unused")
    public static boolean isCompletedSuccessfully(UUID request) {
        synchronized (entries) {
            var req = entries.get(request);
            return req.getResult() instanceof TerminalLaunchResult.ResultSuccess;
        }
    }

    public static void registerPid(UUID request, long pid) throws BeaconClientException {
        TerminalLaunchRequest req;
        synchronized (entries) {
            req = entries.get(request);
        }
        if (req == null) {
            return;
        }
        var byPid = ProcessHandle.of(pid);
        if (byPid.isEmpty()) {
            throw new BeaconClientException("Unable to find terminal child process " + pid);
        }
        var shell = byPid.get().parent().orElseThrow();
        if (req.getShellPid() != -1 && shell.pid() != req.getShellPid()) {
            throw new BeaconClientException("Wrong launch context");
        }
        req.setShellPid(shell.pid());
    }

    public static void waitExchange(UUID request) throws BeaconServerException {
        TerminalLaunchRequest req;
        synchronized (entries) {
            req = entries.get(request);
        }
        if (req == null) {
            return;
        }

        if (req.isSetupCompleted()) {
            submitAsync(req.getRequest(), req.getProcessControl(), req.getConfig(), req.getWorkingDirectory());
        }
        try {
            req.waitForCompletion();
        } finally {
            req.setSetupCompleted(true);
        }
    }

    public static Path launchExchange(UUID request) throws BeaconClientException, BeaconServerException {
        synchronized (entries) {
            var e = entries.values().stream()
                    .filter(entry -> entry.getRequest().equals(request))
                    .findFirst()
                    .orElse(null);
            if (e == null) {
                // It seems like that some terminals might enter a restart loop to try to start an older process again
                // This would spam XPipe continuously with launch requests if we returned an error here
                // Therefore, we just return a new local shell session
                TrackEvent.withTrace("Unknown launch request")
                        .tag("request", request.toString())
                        .handle();
                try (var sc = LocalShell.getShell().start()) {
                    var defaultShell = sc.getShellDialect();
                    var shellExec = defaultShell.getExecutableName();
                    var script = ScriptHelper.createExecScript(
                            sc,
                            sc.getShellDialect()
                                            .getEchoCommand(
                                                    "Unknown "
                                                            + AppNames.ofCurrent()
                                                                    .getName() + " launch request",
                                                    false)
                                    + "\n" + shellExec);
                    return Path.of(script.toString());
                } catch (Exception ex) {
                    throw new BeaconServerException(ex);
                }
            }

            if (!(e.getResult() instanceof TerminalLaunchResult.ResultSuccess)) {
                throw new BeaconClientException("Invalid launch request state " + request);
            }

            return ((TerminalLaunchResult.ResultSuccess) e.getResult()).getTargetScript();
        }
    }

    public static List<String> externalExchange(DataStoreEntryRef<ShellStore> ref, List<String> arguments)
            throws BeaconClientException, BeaconServerException {
        var request = UUID.randomUUID();
        ShellControl session;
        try {
            session = ref.getStore().getOrStartSession();
        } catch (Exception e) {
            throw new BeaconServerException(e);
        }

        ProcessControl control;
        if (arguments.size() > 0) {
            control = session.command(CommandBuilder.of().addAll(arguments));
        } else {
            control = session;
        }

        var config = new TerminalInitScriptConfig(ref.get().getName(), false, TerminalInitFunction.none());
        submitAsync(request, control, config, null);
        waitExchange(request);
        var script = launchExchange(request);
        try (var sc = LocalShell.getShell().start()) {
            var runCommand = ProcessControlProvider.get()
                    .getEffectiveLocalDialect()
                    .getOpenScriptCommand(script.toString())
                    .buildBaseParts(sc);
            var cleaned = runCommand.stream()
                    .map(s -> {
                        if (s.startsWith("\"") && s.endsWith("\"")) {
                            s = s.substring(1, s.length() - 1);
                        } else if (s.startsWith("'") && s.endsWith("'")) {
                            s = s.substring(1, s.length() - 1);
                        }
                        return s;
                    })
                    .toList();
            return cleaned;
        } catch (Exception e) {
            throw new BeaconServerException(e);
        }
    }
}
