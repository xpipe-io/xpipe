package io.xpipe.cli.util;

import picocli.CommandLine;

public class QuietOverride {

    private static boolean quiet = false;

    @CommandLine.Option(
            names = {"-q", "--quiet"},
            description =
                    "The quiet switch indicates that the command must run non-interactively. "
                            + "In case interactivity is required to query further user input, the command will fail if this switch is set.")
    public static void setQuiet(boolean v) {
        quiet = v;
    }

    public static boolean get() {
        return quiet;
    }
}
