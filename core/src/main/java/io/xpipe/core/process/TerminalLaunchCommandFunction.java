package io.xpipe.core.process;

public interface TerminalLaunchCommandFunction {

    CommandBuilder apply(ShellControl shellControl, boolean requiresExecutableFirst, boolean supportsRawArguments, String file, boolean exit);
}
