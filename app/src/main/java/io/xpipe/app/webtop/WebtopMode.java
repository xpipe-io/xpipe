package io.xpipe.app.webtop;

import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.LocalExec;
import io.xpipe.app.util.ThreadHelper;

import java.nio.file.Files;

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
            var file = AppSystemInfo.ofCurrent().getUserHome().resolve(".xpipe", "webtop", "mobile");
            if (!mobile) {
                Files.deleteIfExists(file);
                ExternalApplicationHelper.startAsync(CommandBuilder.of().add("bash", "-c").addQuoted("sleep 2 && /defaults/desktop.sh && plasmashell --replace && xpipe open"));
            } else {
                Files.createDirectories(file.getParent());
                Files.createFile(file);
                ExternalApplicationHelper.startAsync(CommandBuilder.of().add("bash", "-c").addQuoted("sleep 2 && /defaults/mobile.sh && plasmashell --replace && xpipe open"));
            }
        });
    }

    public static boolean isMobile() {
        return mobileMode;
    }
}
