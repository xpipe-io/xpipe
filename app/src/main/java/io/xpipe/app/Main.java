package io.xpipe.app;

import io.xpipe.app.core.AppNames;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.AppOperationMode;

public class Main {

    static void main(String[] args) {
        if (args.length == 1 && args[0].equals("version")) {
            AppProperties.init(args);
            System.out.println(AppProperties.get().getVersion());
            return;
        }

        if (args.length == 1 && (args[0].equals("--help") || args[0].equals("help"))) {
            System.out.println("For a reference on how to use xpipe from the command-line, take a look at https://docs.xpipe.io/cli");
            return;
        }

        AppOperationMode.init(args);
    }
}
