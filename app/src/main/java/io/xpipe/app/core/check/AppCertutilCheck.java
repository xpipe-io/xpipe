package io.xpipe.app.core.check;

import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.OsType;

import java.util.concurrent.TimeUnit;

public class AppCertutilCheck {

    private static boolean getResult() {
        var fc = new ProcessBuilder(AppSystemInfo.getWindows()
                        .getSystemRoot()
                        .resolve("\\System32\\certutil")
                        .toString())
                .redirectErrorStream(true);
        try {
            var proc = fc.start();
            var out = new String(proc.getInputStream().readAllBytes());
            proc.waitFor(1, TimeUnit.SECONDS);
            return proc.exitValue() == 0 && !out.contains("The system cannot execute the specified program");
        } catch (Exception e) {
            return false;
        }
    }

    public static void check() {
        if (AppPrefs.get().disableCertutilUse().get()) {
            return;
        }

        if (OsType.getLocal() != OsType.WINDOWS) {
            return;
        }

        if (LocalShell.getDialect() != ShellDialects.CMD) {
            return;
        }

        if (!getResult()) {
            AppPrefs.get().disableCertutilUse.set(true);
        }
    }
}
