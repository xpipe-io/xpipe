package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;

import java.util.*;

public class TerminalMultiplexerManager {

    private static final Map<UUID, TerminalMultiplexer> connectionHubRequests = new HashMap<>();

    public static Optional<TerminalMultiplexer> getEffectiveMultiplexer() {
        var multiplexer = AppPrefs.get().terminalMultiplexer().getValue();
        return Optional.ofNullable(multiplexer);
    }

    public static boolean requiresNewTerminalSession(UUID requestUuid) {
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
