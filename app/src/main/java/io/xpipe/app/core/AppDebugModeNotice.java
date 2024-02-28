package io.xpipe.app.core;

import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.util.ModuleHelper;

public class AppDebugModeNotice {

    public static void printIfNeeded() {
        if (!ModuleHelper.isImage() || !AppLogs.get().getLogLevel().equals("trace")) {
            return;
        }

        var out = AppLogs.get().getOriginalSysOut();
        var msg =
                """

                ****************************************
                * You are running XPipe in debug mode! *
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
