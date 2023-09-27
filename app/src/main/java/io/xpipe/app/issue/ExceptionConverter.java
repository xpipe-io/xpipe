package io.xpipe.app.issue;

import io.xpipe.core.process.ProcessOutputException;

public class ExceptionConverter {

    public static String convertMessage(Throwable ex) {
        return switch (ex) {
            case ProcessOutputException e -> e.getMessage();
            default -> {
                yield ex.toString();
            }
        };
    }
}
