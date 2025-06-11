package io.xpipe.app.core.check;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.core.process.OsType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class AppTempCheck {

    private static void checkTemp(String tmpdir) {
        Path dir = null;
        if (tmpdir != null) {
            try {
                dir = Path.of(tmpdir);
            } catch (InvalidPathException ignored) {
            }
        }

        if (dir == null || !Files.exists(dir) || !Files.isDirectory(dir)) {
            ErrorEventFactory.fromThrowable(new IOException("Specified temporary directory " + tmpdir
                            + ", set via the environment variable %TEMP% is invalid."))
                    .term()
                    .expected()
                    .handle();
        }
    }

    public static void check() {
        if (!OsType.getLocal().equals(OsType.WINDOWS)) {
            return;
        }

        checkTemp(System.getProperty("java.io.tmpdir"));
        checkTemp(System.getenv("TEMP"));
    }
}
