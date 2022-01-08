package io.xpipe.extension.event;

import io.xpipe.extension.I18n;

import java.io.FileNotFoundException;

public class ExceptionConverter {

    public static String convertMessage(Throwable ex) {
        var msg = ex.getLocalizedMessage();
        if (ex instanceof FileNotFoundException) {
            return I18n.get("fileNotFound", msg);
        }

        return msg;
    }
}
