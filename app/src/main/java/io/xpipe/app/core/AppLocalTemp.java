package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.core.OsType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

public class AppLocalTemp {

    public static Path getLocalTempDataDirectory() {
        var temp = AppSystemInfo.ofCurrent().getTemp().resolve(AppNames.ofCurrent().getKebapName());
        // On Windows and macOS, we already have user specific temp directories
        // Even on macOS as root we will have a unique directory (in contrast to shell controls)
        if (OsType.ofLocal() == OsType.LINUX) {
            try {
                Files.createDirectories(temp);
                // We did not set this in earlier versions. If we are running as a different user, it might fail
                Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rwxrwxrwx"));
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().expected().handle();
            }

            var user = AppSystemInfo.ofCurrent().getUser();
            temp = temp.resolve(user);

            try {
                Files.createDirectories(temp);
                // We did not set this in earlier versions. If we are running as a different user, it might fail
                Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rwx------"));
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().expected().handle();
            }
        } else {
            try {
                Files.createDirectories(temp);
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().expected().handle();
            }
        }

        return temp;
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
