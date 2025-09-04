package io.xpipe.app.core.window;

import io.xpipe.app.platform.PlatformInit;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AppSideWindow {

    public static Optional<ButtonType> showBlockingAlert(Consumer<Alert> c) {
        PlatformInit.init(true);

        Supplier<Alert> supplier = () -> {
            Alert a = createEmptyAlert();
            var s = (Stage) a.getDialogPane().getScene().getWindow();
            s.setOnShown(event -> {
                Platform.runLater(() -> {
                    AppWindowBounds.clampWindow(s).ifPresent(rectangle2D -> {
                        s.setX(rectangle2D.getMinX());
                        s.setY(rectangle2D.getMinY());
                        // Somehow we have to set max size as setting the normal size does not work?
                        s.setMaxWidth(rectangle2D.getWidth());
                        s.setMaxHeight(rectangle2D.getHeight());
                    });
                });
                event.consume();
            });
            AppWindowBounds.fixInvalidStagePosition(s);
            AppWindowStyle.addFontSize(s);
            a.getDialogPane().getScene().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN).match(event)) {
                    s.close();
                    event.consume();
                    return;
                }

                if (event.getCode().equals(KeyCode.ESCAPE)) {
                    s.close();
                    event.consume();
                }
            });
            return a;
        };

        AtomicReference<Optional<ButtonType>> result = new AtomicReference<>();
        if (!Platform.isFxApplicationThread()) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    Alert a = supplier.get();
                    c.accept(a);
                    result.set(a.showAndWait());
                } catch (Throwable t) {
                    result.set(Optional.empty());
                } finally {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }
        } else {
            Alert a = supplier.get();
            c.accept(a);
            result.set(a.showAndWait());
        }
        return result.get();
    }

    public static Alert createEmptyAlert() {
        Alert alert = new Alert(Alert.AlertType.NONE);
        if (AppMainWindow.get() != null) {
            alert.initOwner(AppMainWindow.get().getStage());
        }
        alert.getDialogPane().getScene().setFill(Color.TRANSPARENT);
        var stage = (Stage) alert.getDialogPane().getScene().getWindow();
        AppModifiedStage.prepareStage(stage);
        AppWindowStyle.addIcons(stage);
        AppWindowStyle.addStylesheets(alert.getDialogPane().getScene());
        AppWindowStyle.addNavigationStyleClasses(alert.getDialogPane().getScene());
        return alert;
    }
}
