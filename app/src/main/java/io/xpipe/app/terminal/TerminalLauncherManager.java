package io.xpipe.app.terminal;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.file.BrowserTerminalDockTabModel;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.core.process.ProcessControl;
import io.xpipe.core.process.TerminalInitScriptConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class TerminalLauncherManager {

    private static final SequencedMap<UUID, TerminalLaunchRequest> entries = new LinkedHashMap<>();

    public static void init() {
        if (!TerminalView.isSupported()) {
            return;
        }

        TerminalView.get().addListener(new TerminalView.Listener() {
            @Override
            public void onSessionOpened(TerminalView.Session session) {}

            @Override
            public void onSessionClosed(TerminalView.Session session) {
                var affectedEntry = entries.values().stream().filter(terminalLaunchRequest -> {
                    return terminalLaunchRequest.getRequest().equals(session.getRequest());
                }).findFirst();
                if (affectedEntry.isEmpty()) {
                    return;
                }

                affectedEntry.get().abort();
            }

            @Override
            public void onTerminalOpened(TerminalViewInstance instance) {

            }

            @Override
            public void onTerminalClosed(TerminalViewInstance instance) {

            }
        });
    }

    public static CountDownLatch submitAsync(
            UUID request, ProcessControl processControl, TerminalInitScriptConfig config, String directory)
            throws BeaconClientException {
        synchronized (entries) {
            var req = entries.get(request);
            if (req == null) {
                req = new TerminalLaunchRequest(request, processControl, config, directory, null, false);
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

    public static Path waitExchange(UUID request) throws BeaconClientException, BeaconServerException {
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
