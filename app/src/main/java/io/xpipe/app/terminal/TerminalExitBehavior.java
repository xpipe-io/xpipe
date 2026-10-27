package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;

public record TerminalExitBehavior(TerminalExitAction normalExit, TerminalExitAction failureExit) {

    public static TerminalExitBehavior ofPrefs() {
        return new TerminalExitBehavior(
                AppPrefs.get().terminalNormalExitBehavior().getValue(),
                AppPrefs.get().terminalFailureExitBehavior().getValue());
    }

    public static TerminalExitBehavior close() {
        return new TerminalExitBehavior(TerminalExitAction.CLOSE, TerminalExitAction.CLOSE);
    }

    public static TerminalExitBehavior pause() {
        return new TerminalExitBehavior(TerminalExitAction.PAUSE, TerminalExitAction.PAUSE);
    }

    public static TerminalExitBehavior pauseOnFailure() {
        return new TerminalExitBehavior(TerminalExitAction.CLOSE, TerminalExitAction.PAUSE);
    }
}
