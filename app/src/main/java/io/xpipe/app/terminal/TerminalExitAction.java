package io.xpipe.app.terminal;

import io.xpipe.app.prefs.PrefsChoiceValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TerminalExitAction implements PrefsChoiceValue {
    CLOSE("terminalExitClose"),
    PAUSE("terminalExitPause"),
    RECONNECT("terminalExitReconnect");

    private final String id;
}
