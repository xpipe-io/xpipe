package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.LinuxLibC;
import io.xpipe.app.util.OsType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;

public class AppLocalTemp {

    public static Path getLocalTempDataDirectory() {
        var temp =
                AppSystemInfo.ofCurrent().getTemp().resolve(AppNames.ofCurrent().getKebapName());
        // On Windows and macOS, we already have user specific temp directories
        // Even on macOS as root we will have a unique directory (in contrast to shell controls)
        if (OsType.ofLocal() == OsType.LINUX) {
            try {
                Files.createDirectories(temp);
                var lib = LinuxLibC.getLibrary();
                if (lib.isPresent()) {
                    lib.get().chmod(temp.toString(), 01777);
                }
            } catch (Throwable e) {
                ErrorEventFactory.fromThrowable(e).omit().expected().handle();
            }

            if (Files.isSymbolicLink(temp)) {
                ErrorEventFactory.fromThrowable(new IOException("Invalid file type for " + temp)).term().handle();
                return null;
            }

            var user = AppSystemInfo.ofCurrent().getUser();
            temp = temp.resolve(user);

            if (Files.isSymbolicLink(temp)) {
                ErrorEventFactory.fromThrowable(new IOException("Invalid file type for " + temp)).term().handle();
                return null;
            }

            try {
                Files.createDirectories(temp);
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
