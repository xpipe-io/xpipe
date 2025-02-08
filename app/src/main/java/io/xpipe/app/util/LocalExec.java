package io.xpipe.app.util;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class LocalExec {

    public static Optional<String> readStdoutIfPossible(String... command) {
        try {
            var process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            var out = process.getInputStream().readAllBytes();
            process.waitFor();
            if (process.exitValue() != 0) {
                return Optional.empty();
            } else {
                return Optional.of(new String(out, StandardCharsets.UTF_8));
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return Optional.empty();
        }
    }
}
