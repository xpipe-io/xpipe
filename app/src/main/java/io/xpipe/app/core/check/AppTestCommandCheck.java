package io.xpipe.app.core.check;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.process.ProcessOutputException;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.OsType;

public class AppTestCommandCheck {

    public static void check() throws Exception {
        if (OsType.getLocal() == OsType.WINDOWS) {
            return;
        }

        try (var sc = LocalShell.getShell().start()) {
            try {
                sc.getShellDialect()
                        .directoryExists(
                                sc,
                                AppInstallation.ofCurrent()
                                        .getBaseInstallationPath()
                                        .toString())
                        .execute();
            } catch (ProcessOutputException ex) {
                throw ProcessOutputException.withPrefix(
                        "Installation self test failed. Is your \"test\" shell command working as expected and is the XPipe installation directory accessible?",
                        ex);
            }
        }
    }
}
