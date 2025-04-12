package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;

import java.util.*;

public class TerminalMultiplexerManager {

    private static UUID pendingMultiplexerLaunch;
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
        return Optional.ofNullable(multiplexer);
    }

    public static boolean requiresNewTerminalSession(UUID requestUuid) {
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

        var mult = getEffectiveMultiplexer();
        if (mult.isEmpty()) {
            connectionHubRequests.put(requestUuid, null);
            return true;
        }

        var hasTerminal = TerminalView.get().getSessions().stream()
                .anyMatch(shellSession -> shellSession.getTerminal().isRunning()
                        && mult.get() == connectionHubRequests.get(shellSession.getRequest()));
        connectionHubRequests.put(requestUuid, mult.get());
        return !hasTerminal;
    }
}
