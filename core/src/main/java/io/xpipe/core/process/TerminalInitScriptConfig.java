package io.xpipe.core.process;

import lombok.Value;

@Value
public class TerminalInitScriptConfig {

    String displayName;
    boolean clearScreen;
    boolean hasColor;

    public static TerminalInitScriptConfig ofName(String name) {
        return new TerminalInitScriptConfig(name, true, false);
    }
}
