package io.xpipe.app;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.OperationMode;

public class Main {

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("version")) {
            AppProperties.init();
            System.out.println(AppProperties.get().getVersion());
            return;
        }

        // Since this is not marked as a console application, it will not print anything when you run it in a console on Windows
        if (args.length == 1 && args[0].equals("--help")) {
            System.out.println("""
                               The daemon executable xpiped does not accept any command-line arguments.
                               
                               For a reference on what you can do from the CLI, take a look at the xpipe CLI executable instead.
                               """);
            return;
        }

        OperationMode.init(args);
    }
}
