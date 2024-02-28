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

        // Since this is not marked as a console application, it will not print anything when you run it in a console
        // So sadly there can't be a help command
        //        if (args.length == 1 && args[0].equals("--help")) {
        //            System.out.println("HELP");
        //            return;
        //        }

        OperationMode.init(args);
    }
}
