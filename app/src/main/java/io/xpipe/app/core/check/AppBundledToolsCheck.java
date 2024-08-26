package io.xpipe.app.core.check;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.OsType;

import java.util.concurrent.TimeUnit;

public class AppBundledToolsCheck {

    private static boolean getResult() {
        var fc = new ProcessBuilder("where", "ssh").redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD);
        try {
            var proc = fc.start();
            proc.waitFor(2, TimeUnit.SECONDS);
            return proc.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static void check() {
        if (AppPrefs.get().useBundledTools().get()) {
            return;
        }

        if (!OsType.getLocal().equals(OsType.WINDOWS)) {
            return;
        }

        if (!getResult()) {
            AppPrefs.get().useBundledTools.set(true);
        }
    }
}
