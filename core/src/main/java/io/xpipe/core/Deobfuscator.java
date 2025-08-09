package io.xpipe.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class Deobfuscator {

    public static String deobfuscateToString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String stackTrace = sw.toString();
        stackTrace = stackTrace.replaceAll("at .+/(.+)", "at $1");

        try {
            if (!canDeobfuscate()) {
                return stackTrace;
            }

            var file = Files.createTempFile("xpipe_stacktrace", null);
            Files.writeString(file, stackTrace);
            var proc = new ProcessBuilder(
                            "retrace." + (OsType.getLocal() == OsType.WINDOWS ? "bat" : "sh"),
                            System.getenv("XPIPE_MAPPING"),
                            file.toString())
                    .redirectErrorStream(true);
            var active = proc.start();
            var out = new String(active.getInputStream().readAllBytes()).replaceAll("\r\n", "\n");
            var code = active.waitFor();
            if (code == 0) {
                return out;
            } else {
                System.err.println("Deobfuscation failed: " + out);
            }
        } catch (Exception ex) {
            System.err.println("Deobfuscation failed");
            return stackTrace;
        }

        return stackTrace;
    }

    public static void printStackTrace(Throwable t) {
        var s = deobfuscateToString(t);
        System.err.println(s);
    }

    private static boolean canDeobfuscate() {
        if (!System.getenv().containsKey("XPIPE_MAPPING")) {
            return false;
        }

        var file = Path.of(System.getenv("XPIPE_MAPPING"));
        if (!Files.exists(file)) {
            return false;
        }

        return true;
    }
}
