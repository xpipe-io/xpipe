package io.xpipe.app.issue;

import io.xpipe.extension.event.ErrorEvent;

public interface ErrorHandler {

    void handle(ErrorEvent event);
}
