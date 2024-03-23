package io.xpipe.app.core.check;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.LogErrorHandler;
import javafx.scene.text.Font;

public class AppFontLoadingCheck {

    public static void check() {
        try {
            // This can fail if the found system fonts can somehow not be loaded
            Font.getDefault();
        } catch (Throwable e) {
            var event = ErrorEvent.fromThrowable("Unable to load fonts", e).build();
            // We can't use the normal error handling facility
            // as the platform reports as working but opening windows still does not work
            new LogErrorHandler().handle(event);
            OperationMode.halt(1);
        }
    }
}
