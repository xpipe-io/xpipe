package io.xpipe.app.util;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.core.OsType;
import io.xpipe.core.XPipeInstallation;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;

import java.util.Map;
import java.util.Optional;

public class NativeBridge {

    private static MacOsLibrary macOsLibrary;
    private static boolean loadingFailed;

    public static void init() {
        // Preload
        if (OsType.getLocal() == OsType.MACOS && AppProperties.get().getArch().equals("arm64")) {
            getMacOsLibrary();
        }
    }

    public static Optional<MacOsLibrary> getMacOsLibrary() {
        if (!AppProperties.get().isImage() || !AppProperties.get().isFullVersion() || !AppProperties.get().getArch().equals("arm64")) {
            return Optional.empty();
        }

        if (macOsLibrary == null && !loadingFailed) {
            try {
                System.setProperty(
                        "jna.library.path",
                        XPipeInstallation.getCurrentInstallationBasePath()
                                .resolve("Contents")
                                .resolve("runtime")
                                .resolve("Contents")
                                .resolve("Home")
                                .resolve("lib")
                                .toString());
                var l = Native.load("xpipe_bridge", MacOsLibrary.class, Map.of());
                macOsLibrary = l;
            } catch (Throwable t) {
                ErrorEventFactory.fromThrowable(t).handle();
                loadingFailed = true;
            }
        }
        return Optional.ofNullable(macOsLibrary);
    }

    public interface MacOsLibrary extends Library {

        void setAppearance(NativeLong window, boolean seamlessFrame, boolean dark);
    }
}
