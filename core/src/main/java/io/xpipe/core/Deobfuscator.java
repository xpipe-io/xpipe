package io.xpipe.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Deobfuscator {

    public static void deobfuscate(Throwable throwable) {
        if (!System.getenv().containsKey("XPIPE_MAPPING")) {
            return;
        }

        String deobf = deobfuscateToString(throwable);

        try {
            // "at package.class.method(source.java:123)"
            Pattern tracePattern = Pattern.compile("\\s*at\\s+([\\w.$_]+)\\.([\\w$_]+)\\((.*):(\\d+)\\)(\\n|\\r\\n)");
            Matcher traceMatcher = tracePattern.matcher(deobf);
            List<StackTraceElement> stackTrace = new ArrayList<>();
            while (traceMatcher.find()) {
                String className = traceMatcher.group(1);
                String methodName = traceMatcher.group(2);
                String sourceFile = traceMatcher.group(3);
                int lineNum = Integer.parseInt(traceMatcher.group(4));
                stackTrace.add(new StackTraceElement(className, methodName, sourceFile, lineNum));
            }

            throwable.setStackTrace(stackTrace.toArray(StackTraceElement[]::new));

            // Also deobfuscate any other exceptions
            if (throwable.getCause() != null && throwable.getCause() != throwable) {
                deobfuscate(throwable.getCause());
            }
            for (Throwable suppressed : throwable.getSuppressed()) {
                if (suppressed != throwable) {
                    deobfuscate(suppressed);
                }
            }
        } catch (Throwable ex) {
            System.err.println("Deobfuscation failed");
            ex.printStackTrace();
        }
    }

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

            var file = Files.createTempFile("xpipe_stracktrace", null);
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
