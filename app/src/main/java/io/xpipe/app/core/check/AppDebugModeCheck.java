package io.xpipe.app.core.check;

import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.util.ThreadHelper;

public class AppDebugModeCheck {

    public static void printIfNeeded() {
        if (!AppProperties.get().isImage() || !AppLogs.get().getLogLevel().equals("trace")) {
            return;
        }

        var out = AppLogs.get().getOriginalSysOut();
        var msg =
                """

                  ****************************************
                  * You are running in debug mode!       *
                  * The debug console output can contain *
                  * sensitive information and secrets.   *
                  * Don't share this output via an       *
                  * untrusted website or service.        *
                  ****************************************
                  """;
        out.println(msg);
        ThreadHelper.sleep(1000);
    }
}
