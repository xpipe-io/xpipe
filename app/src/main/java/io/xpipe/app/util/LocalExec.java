package io.xpipe.app.util;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.issue.TrackEvent;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class LocalExec {

    public static void prepareLocalProcessEnvironment(Map<String, String> env) {
        // https://bugs.openjdk.org/browse/JDK-8360500
        env.remove("_JPACKAGE_LAUNCHER");

        // Remove debug mode vars
        env.remove("JAVA_EXEC");
        env.remove("CDS_JVM_OPTS");

        // Remove any custom java vars
        env.remove("_JAVA_OPTIONS");
        env.remove("JAVA_TOOL_OPTIONS");
        env.remove("JDK_JAVA_OPTIONS");

        // Remove dev vars
        env.remove("XPIPE_MAPPING");

        // Add proxy vars
        var proxyMap = HttpProxy.getEnvironmentVariables();
        env.putAll(proxyMap);
    }

    public static Process executeAsync(String... command) {
        var list = Arrays.stream(command).filter(s -> s != null).toList();
        try {
            if (AppProperties.get().isDaemon()) {
                TrackEvent.withTrace("Running local command").tag("command", String.join(" ", list)).handle();
            }

            var pb = new ProcessBuilder(list)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.directory(AppSystemInfo.ofCurrent().getUserHome().toFile());

            var env = pb.environment();
            prepareLocalProcessEnvironment(env);

            return pb.start();
        } catch (Exception ex) {
            if (AppProperties.get().isDaemon()) {
                TrackEvent.withTrace("Local command finished").tag("command", String.join(" ", list)).tag("error", ex.toString()).handle();
            }
            return null;
        }
    }

    public static Optional<String> readStdoutIfPossible(String... command) {
        try {
            if (AppProperties.get().isDaemon()) {
                TrackEvent.withTrace("Running local command").tag("command", String.join(" ", command)).handle();
            }

            var pb = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.directory(AppSystemInfo.ofCurrent().getUserHome().toFile());

            var env = pb.environment();
            prepareLocalProcessEnvironment(env);

            var process = pb.start();
            var out = process.getInputStream().readAllBytes();
            process.waitFor();
            if (process.exitValue() != 0) {
                return Optional.empty();
            } else {
                var s = new String(out, StandardCharsets.UTF_8).strip();
                if (AppProperties.get().isDaemon()) {
                    TrackEvent.withTrace("Local command finished").tag("command", String.join(" ", command)).tag("stdout", s).handle();
                }
                return Optional.of(s);
            }
        } catch (Exception ex) {
            if (AppProperties.get().isDaemon()) {
                TrackEvent.withTrace("Local command finished").tag("command", String.join(" ", command)).tag("error", ex.toString()).handle();
            }
            return Optional.empty();
        }
    }
}
