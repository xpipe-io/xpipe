package io.xpipe.app.util;

import io.xpipe.app.issue.TrackEvent;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class LocalExec {

    public static Optional<String> readStdoutIfPossible(String... command) {
        try {
            TrackEvent.withTrace("Running local command")
                    .tag("command", String.join(" ", command))
                    .handle();

            var pb = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD);
            var env = pb.environment();
            // https://bugs.openjdk.org/browse/JDK-8360500
            env.remove("_JPACKAGE_LAUNCHER");

            var process = pb.start();
            var out = process.getInputStream().readAllBytes();
            process.waitFor();
            if (process.exitValue() != 0) {
                return Optional.empty();
            } else {
                var s = new String(out, StandardCharsets.UTF_8).strip();
                TrackEvent.withTrace("Local command finished")
                        .tag("command", String.join(" ", command))
                        .tag("stdout", s)
                        .handle();
                return Optional.of(s);
            }
        } catch (Exception ex) {
            TrackEvent.withTrace("Local command finished")
                    .tag("command", String.join(" ", command))
                    .tag("error", ex.toString())
                    .handle();
            return Optional.empty();
        }
    }
}
