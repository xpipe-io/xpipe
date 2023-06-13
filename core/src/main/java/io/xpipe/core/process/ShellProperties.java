package io.xpipe.core.process;

import lombok.Value;

@Value
public class ShellProperties {

    ShellDialect dialect;
    boolean ansiEscapes;
}
