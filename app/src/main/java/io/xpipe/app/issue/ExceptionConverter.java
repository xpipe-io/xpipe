package io.xpipe.app.issue;

import io.xpipe.app.core.AppI18n;
import io.xpipe.core.process.ProcessOutputException;

import java.io.FileNotFoundException;

public class ExceptionConverter {

    public static String convertMessage(Throwable ex) {
        var msg = ex.getLocalizedMessage();

        if (!AppI18n.getInstance().isLoaded()) {
            return msg;
        }

        return switch (ex) {
            case ProcessOutputException e -> {
                if (e.getOutput() == null || e.getOutput().isBlank()) {
                    yield e.getMessage();
                } else {
                    yield e.getOutput();
                }
            }
            case StackOverflowError e -> AppI18n.get("app.stackOverflow");
            case OutOfMemoryError e -> AppI18n.get("app.outOfMemory");
            case FileNotFoundException e -> AppI18n.get("app.fileNotFound", msg);
            case NullPointerException e -> AppI18n.get("app.nullPointer");
            case UnsupportedOperationException e -> AppI18n.get("app.unsupportedOperation", msg);
            case ClassNotFoundException e -> AppI18n.get("app.classNotFound", msg);
            default -> {
                if (msg == null || msg.trim().length() == 0) {
                    yield AppI18n.get("app.noInformationAvailable");
                }

                yield msg;
            }
        };
    }
}
