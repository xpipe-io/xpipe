package io.xpipe.app.platform;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.core.OsType;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;

import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

public class NativeBridge {

    private static MacOsLibrary macOsLibrary;
    private static boolean loadingFailed;

    public static void init() {
        // Preload
        if (OsType.ofLocal() == OsType.MACOS && AppProperties.get().getArch().equals("arm64")) {
            getMacOsLibrary();
        }
    }

    public static Optional<MacOsLibrary> getMacOsLibrary() {
        if (!AppProperties.get().isFullVersion()
                || !AppProperties.get().getArch().equals("arm64")) {
            return Optional.empty();
        }

        if (macOsLibrary == null && !loadingFailed) {
            var base = AppProperties.get().isImage() ? AppInstallation.ofCurrent() : AppInstallation.ofDefault();
            var file = base.getBaseInstallationPath()
                    .resolve("Contents")
                    .resolve("runtime")
                    .resolve("Contents")
                    .resolve("Home")
                    .resolve("lib");
            if (!Files.exists(file)) {
                return Optional.empty();
            }

            try {
                System.setProperty("jna.library.path", file.toString());
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
