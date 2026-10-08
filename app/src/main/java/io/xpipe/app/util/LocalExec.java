package io.xpipe.app.util;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.issue.TrackEvent;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LocalExec {

    public static List<String> getEnvironmentVariablesToReset() {
        return List.of(
                // https://bugs.openjdk.org/browse/JDK-8360500
                "_JPACKAGE_LAUNCHER",
                // Debug mode vars
                "JAVA_EXEC",
                "CDS_JVM_OPTS",
                // Custom java vars
                "_JAVA_OPTIONS",
                "JAVA_TOOL_OPTIONS",
                "JDK_JAVA_OPTIONS",
                // Dev vars
                "XPIPE_MAPPING"
        );
    }

    public static void prepareLocalProcessEnvironment(Map<String, String> env) {
        getEnvironmentVariablesToReset().forEach(env::remove);

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
