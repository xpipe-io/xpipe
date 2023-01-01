package io.xpipe.core.util;

import io.xpipe.core.process.ShellTypes;

public class LocalProcess {

    public static boolean executeSimpleBooleanCommand(String cmd) throws Exception {
        var proc = new ProcessBuilder(ShellTypes.getPlatformDefault().executeCommandListWithShell(cmd)).redirectErrorStream(true).redirectOutput(
                ProcessBuilder.Redirect.DISCARD).start();
        return proc.waitFor() == 0;
    }
}
