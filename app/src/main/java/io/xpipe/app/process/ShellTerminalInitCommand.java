package io.xpipe.app.process;

import java.util.Optional;

public interface ShellTerminalInitCommand {

    Optional<String> terminalContent(ShellControl shellControl) throws Exception;

    boolean canPotentiallyRunInDialect(ShellDialect dialect);
}
