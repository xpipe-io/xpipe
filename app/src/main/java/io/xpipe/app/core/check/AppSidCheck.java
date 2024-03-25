package io.xpipe.app.core.check;

import com.sun.jna.Function;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.process.OsType;

public class AppSidCheck {

    public static void check() {
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return;
        }

        try {
            var func = Function.getFunction("c", "setsid");
            func.invoke(new Object[0]);
            TrackEvent.info("Successfully set process sid");
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).omit().handle();
        }
    }
}
