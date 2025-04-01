package io.xpipe.core.process;

import lombok.NonNull;

import java.util.Optional;

public interface ShellTerminalInitCommand {

    default Optional<String> terminalContent(ShellControl shellControl) throws Exception {
        throw new UnsupportedOperationException();
    }

    boolean canPotentiallyRunInDialect(ShellDialect dialect);
}
