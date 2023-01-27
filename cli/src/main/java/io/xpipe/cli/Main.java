package io.xpipe.cli;

import io.xpipe.cli.util.CliHelper;
import io.xpipe.cli.util.CliProperties;
import io.xpipe.cli.util.TerminalHelper;

public class Main {

    private static boolean init;

    public static void main(String[] args) throws Exception {
        mainInternal(args);
    }

    public static int mainInternal(String[] args) throws Exception {
        // Don't initialize multiple times when using tests
        if (!init) {
            CliProperties.init();
            if (!CliHelper.isProduction()) {
                new BuildTimeInitialization();
            }

            if (CliHelper.isProduction()) {
                // Disabled for now as it somehow introduces a huge initial loading delay at the first startup
                // AnsiConsole.systemInstall();
            }
            init = true;
        }

        try (var ignored = TerminalHelper.init()) {
        return XPipeCommand.execute(args);
        }
    }
}
