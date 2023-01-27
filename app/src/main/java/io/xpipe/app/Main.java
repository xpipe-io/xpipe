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

        OperationMode.init(args);
    }
}
