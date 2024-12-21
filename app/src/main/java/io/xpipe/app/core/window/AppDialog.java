package io.xpipe.app.core.window;

import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.PlatformInit;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class AppDialog {

    @Getter
    private static final ObjectProperty<ModalOverlay> modalOverlay = new SimpleObjectProperty<>();

    private static void showMainWindow() {
        PlatformInit.init(true);
        AppMainWindow.initEmpty();
    }

    private static void closeDialog() {
        modalOverlay.setValue(null);
    }

    public static void waitForClose() {
        while (modalOverlay.getValue() != null) {
            ThreadHelper.sleep(10);
        }
    }

    @SneakyThrows
    public static void show(ModalOverlay o) {
        showMainWindow();

        if (!Platform.isFxApplicationThread()) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    modalOverlay.setValue(o);
                } finally {
                    latch.countDown();
                }
            });
            latch.await();
            waitForClose();
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }
        } else {
            modalOverlay.setValue(o);
            var key = new Object();
            modalOverlay.addListener((observable, oldValue, newValue) -> {
                if (oldValue == o && newValue == null) {
                    Platform.exitNestedEventLoop(key, null);
                }
            });
            Platform.enterNestedEventLoop(key);
            waitForClose();
        }
    }
}
