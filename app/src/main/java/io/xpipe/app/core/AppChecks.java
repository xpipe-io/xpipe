package io.xpipe.app.core;

import io.xpipe.extension.event.ErrorEvent;

import java.io.IOException;
import java.nio.file.Files;

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
}
