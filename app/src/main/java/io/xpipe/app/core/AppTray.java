package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.ErrorHandler;
import javafx.application.Platform;
import lombok.Getter;
import lombok.SneakyThrows;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;

public class AppTray {

    private static AppTray INSTANCE;
    private final AppTrayIcon icon;
    @Getter
    private final ErrorHandler errorHandler;

    @SneakyThrows
    private AppTray() {
        this.icon = new AppTrayIcon();
        this.errorHandler = new TrayErrorHandler();
    }

    public static void init() {
        INSTANCE = new AppTray();
    }

    public static AppTray get() {
        return INSTANCE;
    }

    @SneakyThrows
    public void show() {
        // Even though we check at startup, it seems like the support can change at runtime
        if (!SystemTray.isSupported()) {
            return;
        }

        icon.show();
    }

    public void hide() {
        icon.hide();
    }

    private class TrayErrorHandler implements ErrorHandler {

        private Instant lastErrorShown = Instant.MIN;

        @Override
        public void handle(ErrorEvent event) {
            if (event.isOmitted()) {
                return;
            }

            var title = AppI18n.get(event.isTerminal() ? "terminalErrorOccured" : "errorOccured");
            var desc = event.getDescription();
            if (desc == null && event.getThrowable() != null) {
                var tName = event.getThrowable().getClass().getSimpleName();
                desc = AppI18n.get("errorTypeOccured", tName);
            }
            if (desc == null) {
                desc = AppI18n.get("errorNoDetail");
            }

            String finalDesc = desc;
            Platform.runLater(() -> {
                if (Duration.between(lastErrorShown, Instant.now()).getSeconds() < 10) {
                    return;
                }

                lastErrorShown = Instant.now();
                AppTray.this.icon.showErrorMessage(title, finalDesc);
            });
        }
    }
}
