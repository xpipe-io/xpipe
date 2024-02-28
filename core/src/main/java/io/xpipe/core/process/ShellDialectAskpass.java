package io.xpipe.core.process;

import java.util.List;
import java.util.UUID;

public interface ShellDialectAskpass {

    String prepareStderrPassthroughContent(ShellControl sc, UUID requestId, String prefix);

    String prepareFixedContent(ShellControl sc, String fileName, List<String> s) throws Exception;

    String elevateDumbCommand(
            ShellControl shellControl,
            CommandConfiguration command,
            UUID requestId,
            CountDown countDown,
            String message)
            throws Exception;

    String elevateTerminalCommandWithPreparedAskpass(ShellControl shellControl, String command, String prefix)
            throws Exception;
}
