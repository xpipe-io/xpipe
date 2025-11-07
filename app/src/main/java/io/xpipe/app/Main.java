package io.xpipe.app;

import io.xpipe.app.core.AppNames;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.AppOperationMode;

public class Main {

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("version")) {
            AppProperties.init(args);
            System.out.println(AppProperties.get().getVersion());
            return;
        }

        // Since this is not marked as a console application, it will not print anything when you run it in a console on
        // Windows
        if (args.length == 1 && args[0].equals("--help")) {
            System.out.println(
                    """
                               The daemon executable %s does not accept any command-line arguments.

                               For a reference on how to use xpipe from the command-line, take a look at https://docs.xpipe.io/cli.
                               """
                            .formatted(AppNames.ofCurrent().getExecutableName()));
            return;
        }

        AppOperationMode.init(args);
    }
}
