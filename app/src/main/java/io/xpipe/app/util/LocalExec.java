package io.xpipe.app.util;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class LocalExec {

    public static Optional<String> readStdoutIfPossible(String... command) {
        try {
            TrackEvent.withTrace("Running local command").tag("command", String.join(" ", command)).handle();
            var process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            var out = process.getInputStream().readAllBytes();
            process.waitFor();
            if (process.exitValue() != 0) {
                return Optional.empty();
            } else {
                var s = new String(out, StandardCharsets.UTF_8);
                TrackEvent.withTrace("Local command finished").tag("command", String.join(" ", command)).tag("stdout", s).handle();
                return Optional.of(s);
            }
        } catch (Exception ex) {
            TrackEvent.withTrace("Local command finished").tag("command", String.join(" ", command)).tag("error", ex.toString()).handle();
            return Optional.empty();
        }
    }
}
