package io.xpipe.core.process;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class ShellProperties {

    public ShellProperties(ShellDialect dialect, boolean ansiEscapes) {
        this.dialect = dialect;
        this.ansiEscapes = ansiEscapes;
        this.supported = true;
    }

    ShellDialect dialect;
    boolean ansiEscapes;
    boolean supported;
}
