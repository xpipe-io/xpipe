package io.xpipe.app.terminal;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.SecretManager;
import io.xpipe.app.util.SecretQueryProgress;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;

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
            throw new BeaconClientException("Unknown launch request " + request);
        }
        var byPid = ProcessHandle.of(pid);
        if (byPid.isEmpty()) {
            throw new BeaconClientException("Unable to find terminal child process " + pid);
        }
        var shell = byPid.get().parent().orElseThrow();
        if (req.getPid() != -1 && shell.pid() != req.getPid()) {
            throw new BeaconClientException("Wrong launch context");
        }
        req.setPid(shell.pid());
    }

    public static Path waitExchange(UUID request) throws BeaconClientException, BeaconServerException {
        TerminalLaunchRequest req;
        synchronized (entries) {
            req = entries.get(request);
        }
        if (req == null) {
            throw new BeaconClientException("Unknown launch request " + request);
        }

        if (req.isSetupCompleted()) {
            submitAsync(req.getRequest(), req.getProcessControl(), req.getConfig(), req.getWorkingDirectory());
        }
        try {
            return req.waitForCompletion();
        } finally {
            req.setSetupCompleted(true);
        }
    }

    public static Path launchExchange(UUID request) throws BeaconClientException {
        synchronized (entries) {
            var e = entries.values().stream()
                    .filter(entry -> entry.getRequest().equals(request))
                    .findFirst()
                    .orElse(null);
            if (e == null) {
                throw new BeaconClientException("Unknown launch request " + request);
            }

            if (!(e.getResult() instanceof TerminalLaunchResult.ResultSuccess)) {
                throw new BeaconClientException("Invalid launch request state " + request);
            }

            return ((TerminalLaunchResult.ResultSuccess) e.getResult()).getTargetScript();
        }
    }


    public static List<String> externalExchange(DataStoreEntryRef<ShellStore> ref, List<String> arguments) throws BeaconClientException, BeaconServerException {
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
            var runCommand = ProcessControlProvider.get().getEffectiveLocalDialect().getOpenScriptCommand(script.toString()).buildBaseParts(sc);
            var cleaned = runCommand.stream().map(s -> {
                if (s.startsWith("\"") && s.endsWith("\"")) {
                    s = s.substring(1, s.length() - 1);
                } else if (s.startsWith("'") && s.endsWith("'")) {
                    s = s.substring(1, s.length() - 1);
                }
                return s;
            }).toList();
            return cleaned;
        } catch (Exception e) {
            throw new BeaconServerException(e);
        }
    }
}
