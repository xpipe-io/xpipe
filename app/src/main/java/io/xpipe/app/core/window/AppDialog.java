package io.xpipe.app.core.window;

import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.util.PlatformInit;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Duration;

import lombok.Getter;

public class AppDialog {

    @Getter
    private static final ObjectProperty<ModalOverlay> modalOverlay = new SimpleObjectProperty<>();

    private static void showMainWindow() {
        PlatformInit.init(true);
        PlatformThread.runLaterIfNeededBlocking(() -> {
            AppMainWindow.initEmpty(true);
        });
    }

    private static void closeDialog() {
        modalOverlay.setValue(null);
    }

    public static void waitForClose() {
        while (modalOverlay.getValue() != null) {
            ThreadHelper.sleep(10);
        }
    }

    public static void showAndWait(ModalOverlay o) {
        showMainWindow();
        if (!Platform.isFxApplicationThread()) {
            PlatformThread.runLaterIfNeededBlocking(() -> {
                modalOverlay.setValue(o);
            });
            waitForClose();
            ThreadHelper.sleep(200);
        } else {
            var key = new Object();
            PlatformThread.runLaterIfNeededBlocking(() -> {
                modalOverlay.setValue(o);
                modalOverlay.addListener((observable, oldValue, newValue) -> {
                    if (oldValue == o && newValue == null) {
                        var transition = new PauseTransition(Duration.millis(200));
                        transition.setOnFinished(e -> {
                            Platform.exitNestedEventLoop(key, null);
                        });
                        transition.play();
                    }
                });
            });
            Platform.enterNestedEventLoop(key);
            waitForClose();
        }
    }
}
