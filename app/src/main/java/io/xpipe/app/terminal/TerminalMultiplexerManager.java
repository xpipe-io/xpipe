package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.OsType;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class TerminalMultiplexerManager {

    private static final Map<UUID, TerminalMultiplexer> connectionHubRequests = new HashMap<>();
    private static UUID pendingMultiplexerLaunch;
    private static Instant lastCheck = Instant.now();

    public static void registerMultiplexerLaunch(UUID uuid) {
        pendingMultiplexerLaunch = uuid;
        var listener = new TerminalView.Listener() {
            @Override
            public void onSessionOpened(TerminalView.ShellSession session) {
                if (session.getRequest().equals(pendingMultiplexerLaunch)) {
                    pendingMultiplexerLaunch = null;
                    TerminalView.get().removeListener(this);
                }
            }
        };
        TerminalView.get().addListener(listener);
    }

    public static Optional<TerminalMultiplexer> getEffectiveMultiplexer() {
        var multiplexer = AppPrefs.get().terminalMultiplexer().getValue();
        if (multiplexer == null) {
            return Optional.empty();
        }

        if (OsType.ofLocal() == OsType.WINDOWS) {
            var hasProxy = AppPrefs.get().terminalProxy().getValue() != null;
            if (!hasProxy) {
                return Optional.empty();
            }
        }

        return Optional.of(multiplexer);
    }

    public static void synchronizeMultiplexerLaunchTiming() {
        var mult = getEffectiveMultiplexer();
        if (mult.isEmpty()) {
            return;
        }

        // Wait if we are currently opening a new multiplexer
        if (pendingMultiplexerLaunch != null) {
            // Wait for max 10s
            for (int i = 0; i < 100; i++) {
                if (pendingMultiplexerLaunch == null) {
                    break;
                }

                ThreadHelper.sleep(100);
            }
            // Give multiplexer a second to start in terminal
            ThreadHelper.sleep(1000);
        }

        // Synchronize between multiple existing tab launches as well as some multiplexers might break there
        var elapsed = Duration.between(lastCheck, Instant.now()).toMillis();
        if (elapsed < 1000) {
            ThreadHelper.sleep(1000 - elapsed);
        }

        lastCheck = Instant.now();
    }

    public static void registerSessionLaunch(UUID launchRequestUuid, TerminalLaunchConfiguration configuration) {
        var mult = getEffectiveMultiplexer();

        for (TerminalPaneConfiguration pane : configuration.getPanes()) {
            TerminalView.get().addSubstitution(pane.getRequest(), launchRequestUuid);
            if (mult.isEmpty()) {
                connectionHubRequests.put(pane.getRequest(), null);
                return;
            }

            connectionHubRequests.put(pane.getRequest(), mult.orElse(null));
        }
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
