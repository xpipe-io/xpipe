package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.OsType;

import java.util.*;

public class TerminalMultiplexerManager {

    private static final Map<UUID, TerminalMultiplexer> connectionHubRequests = new HashMap<>();
    private static UUID pendingMultiplexerLaunch;
    private static UUID runningMultiplexerContainer;
    private static Boolean availableOnWindows;

    public static void registerMultiplexerContainerLaunch(UUID uuid) {
        pendingMultiplexerLaunch = uuid;
        var listener = new TerminalView.Listener() {
            @Override
            public void onSessionOpened(TerminalView.ShellSession session) {
                if (session.getRequest().equals(pendingMultiplexerLaunch)) {
                    pendingMultiplexerLaunch = null;
                    runningMultiplexerContainer = uuid;
                }
            }

            @Override
            public void onSessionClosed(TerminalView.ShellSession session) {
                // Technically, due to how multiplexers handle, this can only be 0 or 1
                // as it only tracks the base shell session the multiplexer runs in
                var left = TerminalView.get().getSessions().stream()
                        .filter(shellSession -> {
                            return connectionHubRequests.containsKey(shellSession.getRequest())
                                    && shellSession.getTerminal().isRunning();
                        })
                        .count();
                if (left == 0) {
                    runningMultiplexerContainer = null;
                    TerminalView.get().removeListener(this);
                }
            }
        };
        TerminalView.get().addListener(listener);
    }

    public static boolean isAvailableOnWindows() {
        // TODO: It seems like zellij is still broken on Windows
        // Check back later
        if (true) {
            return false;
        }

        if (availableOnWindows != null) {
            return availableOnWindows;
        }

        try {
            return (availableOnWindows = LocalShell.getShell().view().findProgram("zellij").isPresent());
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return (availableOnWindows = false);
        }
    }

    public static Optional<TerminalMultiplexer> getEffectiveMultiplexer() {
        var multiplexer = AppPrefs.get().terminalMultiplexer().getValue();
        if (multiplexer == null) {
            return Optional.empty();
        }

        var terminal = AppPrefs.get().terminalType().getValue();
        if (!(terminal instanceof TrackableTerminalType)) {
            return Optional.empty();
        }

        // Don't apply multiplexer on webtop if it is not installed yet
        if (AppDistributionType.get() == AppDistributionType.WEBTOP) {
            try {
                if (!multiplexer.isSupported()) {
                    return Optional.empty();
                }
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
            }
        }


        return Optional.of(multiplexer);
    }

    public static void waitForMultiplexerStartup() {
        var mult = getEffectiveMultiplexer();
        if (mult.isEmpty()) {
            return;
        }

        // Wait if we are currently opening a new multiplexer
        if (pendingMultiplexerLaunch != null) {
            // Wait for max 30s
            // Multiplexer launches in WSL may take a while
            for (int i = 0; i < 300; i++) {
                if (pendingMultiplexerLaunch == null) {
                    // Give it a bit more time if it just started
                    ThreadHelper.sleep(1000);
                    break;
                }

                ThreadHelper.sleep(100);
            }

            // We timed out
            pendingMultiplexerLaunch = null;
        }
    }

    public static void registerSessionLaunch(TerminalLaunchConfiguration configuration) {
        var mult = getEffectiveMultiplexer();

        for (TerminalPaneConfiguration pane : configuration.getPanes()) {
            if (mult.isEmpty()) {
                connectionHubRequests.put(pane.getRequest(), null);
                return;
            }

            connectionHubRequests.put(pane.getRequest(), mult.orElse(null));
        }
    }

    public static Optional<UUID> getActiveMultiplexerContainerRequest() {
        var mult = getEffectiveMultiplexer();
        if (mult.isEmpty()) {
            return Optional.empty();
        }

        // Check for changed multiplexer
        var session = TerminalView.get().getSessions().stream()
                .filter(shellSession -> shellSession.getTerminal().isRunning()
                        && mult.get() == connectionHubRequests.get(shellSession.getRequest()))
                .findFirst();
        if (session.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(runningMultiplexerContainer);
    }

    public static Optional<TerminalView.TerminalSession> getActiveMultiplexerSession() {
        var mult = getEffectiveMultiplexer();
        if (mult.isEmpty()) {
            return Optional.empty();
        }

        var session = TerminalView.get().getSessions().stream()
                .filter(shellSession -> shellSession.getTerminal().isRunning()
                        && mult.get() == connectionHubRequests.get(shellSession.getRequest()))
                .findFirst();
        return session.map(shellSession -> shellSession.getTerminal());
    }
}
