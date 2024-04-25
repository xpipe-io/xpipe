package io.xpipe.core.process;

import lombok.Value;

@Value
public class TerminalInitScriptConfig {

    String displayName;
    boolean clearScreen;
    TerminalInitFunction terminalSpecificCommands;

    public static TerminalInitScriptConfig ofName(String name) {
        return new TerminalInitScriptConfig(name, true, TerminalInitFunction.none());
    }
}
