package io.xpipe.app.core.check;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.LocalExec;
import io.xpipe.core.OsType;

public class AppAndroidLinuxTerminalCheck {

    public static void check() {
        if (OsType.ofLocal() != OsType.LINUX) {
            return;
        }

        if (!AppProperties.get().isNewBuildSession()) {
            return;
        }

        var uname = LocalExec.readStdoutIfPossible("uname", "-a");
        if (uname.isPresent() && uname.get().contains("android") && uname.get().contains("aarch64")) {
            AppPrefs.get().setFromExternal(AppPrefs.get().limitedTouchscreenMode(), true);
        }
    }
}
