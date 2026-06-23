package io.xpipe.app.webtop;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.LocalExec;
import io.xpipe.app.util.ThreadHelper;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class WebtopMode {

    private static boolean mobileMode = false;

    public static void init() {
        var file = AppSystemInfo.ofCurrent().getUserHome().resolve(".xpipe", "webtop", "mobile");
        if (Files.exists(file)) {
            mobileMode = true;
        } else {
            mobileMode = false;
        }
    }

    public static void set(boolean mobile) {
        AppOperationMode.executeAfterShutdown(() -> {
            var exec = AppInstallation.ofCurrent().getCliExecutablePath();
            if (!mobile) {
                ExternalApplicationHelper.startAsync(CommandBuilder.of().add("bash", "-c").addQuoted("/defaults/desktop.sh && /defaults/reload.sh && /defaults/waitx.sh && " + exec + " open"));
            } else {
                ExternalApplicationHelper.startAsync(CommandBuilder.of().add("bash", "-c").addQuoted("/defaults/mobile.sh && /defaults/reload.sh && /defaults/waitx.sh && " + exec + " open"));
            }
        });
    }

    public static boolean isMobile() {
        return mobileMode;
    }
}
