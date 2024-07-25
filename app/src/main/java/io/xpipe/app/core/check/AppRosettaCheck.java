package io.xpipe.app.core.check;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.OsType;

public class AppRosettaCheck {

    public static void check() throws Exception {
        if (OsType.getLocal() != OsType.MACOS) {
            return;
        }

        if (!AppProperties.get().getArch().equals("x86_64")) {
            return;
        }

        var ret = LocalShell.getShell().executeSimpleStringCommand("sysctl -n sysctl.proc_translated");
        if (ret.equals("1")) {
            ErrorEvent.fromMessage("You are running the Intel version of XPipe on an Apple Silicon system."
                    + " There is a native build available that comes with much better performance."
                    + " Please install that one instead.");
        }
    }
}
