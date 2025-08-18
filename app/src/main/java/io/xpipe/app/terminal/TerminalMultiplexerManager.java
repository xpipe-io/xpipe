package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.OsType;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class TerminalMultiplexerManager {

    private static UUID pendingMultiplexerLaunch;
    private static Instant lastCheck = Instant.now();
    private static final Map<UUID, TerminalMultiplexer> connectionHubRequests = new HashMap<>();

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

        if (OsType.getLocal() == OsType.WINDOWS) {
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

    public static Optional<TerminalView.TerminalSession> getActiveTerminalSession(UUID requestUuid) {
        var mult = getEffectiveMultiplexer();
        if (mult.isEmpty()) {
            connectionHubRequests.put(requestUuid, null);
            return Optional.empty();
        }

        var session = TerminalView.get().getSessions().stream()
                .filter(shellSession -> shellSession.getTerminal().isRunning()
                        && mult.get() == connectionHubRequests.get(shellSession.getRequest()))
                .findFirst();
        connectionHubRequests.put(requestUuid, mult.get());
        return session.map(shellSession -> shellSession.getTerminal());
    }
}
