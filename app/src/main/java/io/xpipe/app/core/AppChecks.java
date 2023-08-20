package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.OsType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class AppChecks {

    public static void checkDirectoryPermissions() {
        var dataDirectory = AppProperties.get().getDataDir();

        try {
            Files.createDirectories(dataDirectory);
            var testDirectory = dataDirectory.resolve("permissions_check");
            Files.createDirectories(testDirectory);
            Files.delete(testDirectory);
            // if (true) throw new IOException();
        } catch (IOException e) {
            ErrorEvent.fromThrowable(
                            new IOException(
                                    "Unable to access directory " + dataDirectory
                                            + ". Please make sure that you have the appropriate permissions and no Antivirus program is blocking the access. "
                                            + "In case you use cloud storage, verify that your cloud storage is working and you are logged in."))
                    .term()
                    .handle();
        }
    }

    private static void checkTemp(String tmpdir) {
        Path dir = null;
        try {
            dir = Path.of(tmpdir);
        } catch (InvalidPathException ignored) {
        }

        if (dir == null || !Files.exists(dir) || !Files.isDirectory(dir) || !Files.isWritable(dir)) {
            ErrorEvent.fromThrowable(
                            new IOException("Specified temporary directory " + tmpdir + ", set via the environment variable %TEMP% is invalid."))
                    .term()
                    .handle();
        }
    }

    public static void checkTemp() {
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            checkTemp(System.getProperty("java.io.tmpdir"));
            checkTemp(System.getenv("TEMP"));
        }
    }
}
