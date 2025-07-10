package io.xpipe.app.core.check;

import io.xpipe.app.process.ProcessOutputException;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.OsType;
import io.xpipe.core.XPipeInstallation;

public class AppTestCommandCheck {

    public static void check() throws Exception {
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return;
        }

        try (var sc = LocalShell.getShell().start()) {
            try {
                sc.getShellDialect()
                        .directoryExists(
                                sc,
                                XPipeInstallation.getCurrentInstallationBasePath()
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
