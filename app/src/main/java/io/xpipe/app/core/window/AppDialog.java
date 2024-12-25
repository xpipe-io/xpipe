package io.xpipe.app.core.window;

import atlantafx.base.layout.ModalBox;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.PlatformInit;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.util.Duration;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

public class AppDialog {

    @Getter
    private static final ObjectProperty<ModalOverlay> modalOverlay = new SimpleObjectProperty<>();

    private static void showMainWindow() {
        PlatformInit.init(true);
        AppMainWindow.init(true);
    }

    public static void closeDialog(ModalOverlay overlay) {
        if (modalOverlay.get() == overlay) {
            modalOverlay.setValue(null);
        }
    }

    public static void waitForClose() {
        while (modalOverlay.getValue() != null) {
            ThreadHelper.sleep(10);
        }
    }

    public static void showAndWait(ModalOverlay o) {
        showMainWindow();
        waitForClose();
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

    public static void show(ModalOverlay o, boolean wait, boolean replaceExisting) {
        showMainWindow();
        if (!replaceExisting) {
            waitForClose();
        }
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
                            if (wait) {
                                Platform.exitNestedEventLoop(key, null);
                            }
                        });
                        transition.play();
                    }
                });
            });
            if (wait) {
                Platform.enterNestedEventLoop(key);
                waitForClose();
            }
        }
    }

    public static Comp<?> dialogText(String s) {
        return Comp.of(() -> {
                    var text = new Text(s);
                    text.setWrappingWidth(450);
                    AppFont.medium(text);
                    var sp = new StackPane(text);
                    return sp;
                })
                .prefWidth(450);
    }

    public static boolean confirm(String translationKey) {
        var confirmed = new AtomicBoolean(false);
        var content = dialogText(AppI18n.get(translationKey + "Content"));
        var modal = ModalOverlay.of(translationKey + "Title", content);
        modal.addButton(ModalButton.cancel());
        modal.addButton(ModalButton.ok(() -> confirmed.set(true)));
        showAndWait(modal);
        return confirmed.get();
    }
}
