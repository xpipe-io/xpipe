package io.xpipe.core.process;

import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.FailableFunction;

public interface TerminalLaunchCommandFunction {

    CommandBuilder apply(ShellControl shellControl, boolean requiresExecutableFirst, boolean supportsRawArguments, String file, boolean exit) throws Exception;
}
