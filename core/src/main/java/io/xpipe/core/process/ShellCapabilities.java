package io.xpipe.core.process;

import lombok.Value;

@Value
public class ShellCapabilities {

    public static ShellCapabilities all() {
        return new ShellCapabilities(true, true, true);
    }

    boolean canReadFiles;
    boolean canWriteFiles;
    boolean canListFiles;
}
