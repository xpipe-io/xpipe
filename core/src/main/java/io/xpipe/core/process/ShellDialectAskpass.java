package io.xpipe.core.process;

import java.util.List;
import java.util.UUID;

public interface ShellDialectAskpass {

    String prepareStderrPassthroughContent(ShellControl sc, UUID requestId, String prefix);

    String prepareFixedContent(ShellControl sc, String fileName, List<String> s) throws Exception;

    String elevateDumbCommand(
            ShellControl shellControl,
            UUID requestId,
            ElevationHandler handler,
            CountDown countDown,
            String message, String user, CommandConfiguration command
    )
            throws Exception;

    String elevateTerminalCommandWithPreparedAskpass(
            ShellControl shellControl, ElevationHandler handler, String command, String prefix, String user) throws Exception;
}
