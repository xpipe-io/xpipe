package io.xpipe.extension.event;

import io.xpipe.extension.I18n;

import java.io.FileNotFoundException;

public class ExceptionConverter {

    public static String convertMessage(Throwable ex) {
        var msg = ex.getLocalizedMessage();

        if (!I18n.INSTANCE.isLoaded()) {
            return msg;
        }

        return switch (ex) {
            case StackOverflowError e -> I18n.get("extension.stackOverflow");
            case OutOfMemoryError e -> I18n.get("extension.outOfMemory");
            case FileNotFoundException e -> I18n.get("extension.fileNotFound", msg);
            case NullPointerException e -> I18n.get("extension.nullPointer");
            case UnsupportedOperationException e -> I18n.get("extension.unsupportedOperation", msg);
            case ClassNotFoundException e -> I18n.get("extension.classNotFound", msg);
            default -> {
                if (msg == null || msg.trim().length() == 0) {
                    yield I18n.get("extension.noInformationAvailable");
                }

                yield msg;
            }
        };
    }
}
