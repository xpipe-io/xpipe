package io.xpipe.app.core.check;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.core.OsType;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AppDirectoryPermissionsCheck {

    public static void checkDirectory(Path dataDirectory) {
        try {
            FileUtils.forceMkdir(dataDirectory.toFile());
            var testDirectory = dataDirectory.resolve("permissions_check");
            FileUtils.forceMkdir(testDirectory.toFile());
            if (!Files.exists(testDirectory)) {
                throw new IOException("Directory creation in " + dataDirectory + " failed silently");
            }
            Files.delete(testDirectory);
            // if (true) throw new IOException();
        } catch (IOException e) {
            var message = "Unable to access directory " + dataDirectory + ".";
            if (OsType.getLocal() == OsType.WINDOWS) {
                message +=
                        " Please make sure that you have the appropriate permissions and no Antivirus program is blocking the access. "
                                + "In case you use cloud storage, verify that your cloud storage is working and you are logged in.";
            }
            ErrorEventFactory.fromThrowable(message, e).term().expected().handle();
        }
    }
}
