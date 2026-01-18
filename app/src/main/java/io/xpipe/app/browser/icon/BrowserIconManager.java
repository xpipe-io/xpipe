package io.xpipe.app.browser.icon;

import io.xpipe.app.core.AppImages;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.core.window.AppMainWindow;
import org.apache.commons.io.FilenameUtils;

public class BrowserIconManager {

    private static boolean loaded;

    public static synchronized void init() {
        if (!loaded) {
            BrowserIconFileType.loadDefinitions();
            BrowserIconDirectoryType.loadDefinitions();
            loaded = true;
        }
    }

    public static void loadIfNecessary(String s) {
        var res = AppMainWindow.get().displayScale().get() == 1.0 ? "24" : "40";
        var name = "browser/" + FilenameUtils.getBaseName(s) + "-" + res + ".png";
        AppImages.loadImage(AppResources.MAIN_MODULE, name, name);
    }
}
