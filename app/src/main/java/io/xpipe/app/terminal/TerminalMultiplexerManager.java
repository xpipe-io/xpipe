package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;

import java.util.*;

public class TerminalMultiplexerManager {

    private static final Set<UUID> connectionHubRequests = new HashSet<>();

    public static Optional<TerminalMultiplexer> getEffectiveMultiplexer() {
        var multiplexer = AppPrefs.get().terminalMultiplexer().getValue();
        return Optional.ofNullable(multiplexer);
    }

    public static boolean requiresNewTerminalSession(UUID requestUuid) {
        if (getEffectiveMultiplexer().isEmpty()) {
            connectionHubRequests.add(requestUuid);
            return true;
        }

        var hasTerminal = TerminalView.get().getSessions().stream()
                .anyMatch(shellSession -> shellSession.getTerminal().isRunning()
                        && connectionHubRequests.contains(shellSession.getRequest()));
        connectionHubRequests.add(requestUuid);
        return !hasTerminal;
    }
}
