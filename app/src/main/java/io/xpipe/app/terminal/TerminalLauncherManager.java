package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.core.process.ProcessControl;
import io.xpipe.core.process.TerminalInitScriptConfig;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.UUID;
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
            UUID request, ProcessControl processControl, TerminalInitScriptConfig config, String directory)
            throws BeaconClientException {
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

    public static Path waitExchange(UUID request, long pid) throws BeaconClientException, BeaconServerException {
        TerminalLaunchRequest req;
        synchronized (entries) {
            req = entries.get(request);
        }
        if (req == null) {
            throw new BeaconClientException("Unknown launch request " + request);
        }
        if (req.isSetupCompleted() && AppPrefs.get().dontAllowTerminalRestart().get()) {
            throw new BeaconClientException("Terminal session restarts have been disabled in the security settings");
        }

        var shell = ProcessHandle.of(pid).orElseThrow().parent().orElseThrow();
        if (req.getPid() != -1 && shell.pid() != req.getPid()) {
            throw new BeaconClientException("Wrong launch context");
        }
        req.setPid(shell.pid());

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
}
