package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.LinuxLibC;
import io.xpipe.app.util.OsType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

public class AppLocalTemp {

    public static Path getLocalTempDataDirectory() {
        // On Windows and macOS, we already have user specific temp directories
        // Even on macOS as root we will have a unique directory (in contrast to shell controls)
        if (OsType.ofLocal() == OsType.LINUX) {
            var temp = AppSystemInfo.ofCurrent().getTemp()
                    .resolve(AppNames.ofCurrent().getKebapName() + "-" + AppSystemInfo.ofLinux().getUser());
            try {
                Files.createDirectories(temp);
            } catch (Throwable e) {
                // We can go on without a temp dir, but we can't go on with an existing temp dir that we don't own
                ErrorEventFactory.fromThrowable(e).omit().expected().handle();
            }

            if (Files.isSymbolicLink(temp)) {
                ErrorEventFactory.fromThrowable(new IOException("Invalid file type for " + temp))
                        .term()
                        .handle();
                return null;
            }

            try {
                if (Files.isDirectory(temp)) {
                    Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rwx------"));
                }
                return temp;
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).description("Unable to set temp dir permissions " + temp).term().handle();
                return null;
            }
        } else {
            var temp =
                    AppSystemInfo.ofCurrent().getTemp().resolve(AppNames.ofCurrent().getKebapName());
            try {
                Files.createDirectories(temp);
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().expected().handle();
            }
            return temp;
        }
    }

    public static Path getLocalTempDataDirectory(String sub) {
        var path = getLocalTempDataDirectory().resolve(sub);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            ErrorEventFactory.fromThrowable(e).expected().omit().handle();
        }
        return path;
    }
}
