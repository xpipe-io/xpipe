package io.xpipe.core.process;

import lombok.Value;

@Value
public class TerminalInitScriptConfig {

    public static TerminalInitScriptConfig ofName(String name) {
        return new TerminalInitScriptConfig(name, true, false);
    }

    String displayName;
    boolean clearScreen;
    boolean hasColor;
}
