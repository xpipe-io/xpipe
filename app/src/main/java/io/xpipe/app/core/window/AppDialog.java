package io.xpipe.app.core.window;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.PlatformInit;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.ThreadHelper;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.util.Duration;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

public class AppDialog {

    @Getter
    private static final ObservableList<ModalOverlay> modalOverlays = FXCollections.observableArrayList();

    private static void showMainWindow() {
        PlatformInit.init(true);
        AppMainWindow.init(true);
    }

    public static void closeDialog(ModalOverlay overlay) {
        PlatformThread.runLaterIfNeeded(() -> {
            synchronized (modalOverlays) {
                modalOverlays.remove(overlay);
            }
        });
    }

    public static void waitForAllDialogsClose() {
        while (!modalOverlays.isEmpty()) {
            ThreadHelper.sleep(10);
        }
    }

    private static void waitForDialogClose(ModalOverlay overlay) {
        while (modalOverlays.contains(overlay)) {
            ThreadHelper.sleep(10);
        }
    }

    public static void show(ModalOverlay o) {
        show(o, false);
    }

    public static void hide(ModalOverlay o) {
        PlatformThread.runLaterIfNeeded(() -> {
            modalOverlays.remove(o);
        });
    }

    public static void showAndWait(ModalOverlay o) {
        show(o, true);
    }

    public static void show(ModalOverlay o, boolean wait) {
        showMainWindow();
        if (!Platform.isFxApplicationThread()) {
            PlatformThread.runLaterIfNeededBlocking(() -> {
                synchronized (modalOverlays) {
                    modalOverlays.add(o);
                }
            });
            if (wait) {
                waitForDialogClose(o);
            }
            ThreadHelper.sleep(200);
        } else {
            var key = new Object();
            PlatformThread.runLaterIfNeededBlocking(() -> {
                synchronized (modalOverlays) {
                    modalOverlays.add(o);
                    modalOverlays.addListener(new ListChangeListener<>() {
                        @Override
                        public void onChanged(Change<? extends ModalOverlay> c) {
                            if (!c.getList().contains(o)) {
                                var transition = new PauseTransition(Duration.millis(200));
                                transition.setOnFinished(e -> {
                                    if (wait) {
                                        PlatformThread.exitNestedEventLoop(key);
                                    }
                                });
                                transition.play();
                                modalOverlays.removeListener(this);
                            }
                        }
                    });
                }
            });
            if (wait) {
                PlatformThread.enterNestedEventLoop(key);
                waitForDialogClose(o);
            }
        }
    }

    public static Comp<?> dialogTextKey(String s) {
        return dialogText(AppI18n.observable(s));
    }

    public static Comp<?> dialogText(String s) {
        return Comp.of(() -> {
                    var text = new Text(s);
                    text.getStyleClass().add("dialog-text");
                    var sp = new StackPane(text);
                    text.wrappingWidthProperty().bind(sp.prefWidthProperty());
                    return sp;
                })
                .prefWidth(450);
    }

    public static Comp<?> dialogText(ObservableValue<String> s) {
        return Comp.of(() -> {
                    var text = new Text();
                    text.getStyleClass().add("dialog-text");
                    text.textProperty().bind(s);
                    var sp = new StackPane(text);
                    text.wrappingWidthProperty().bind(sp.prefWidthProperty());
                    return sp;
                })
                .prefWidth(450);
    }

    public static boolean confirm(String translationKey) {
        var confirmed = new AtomicBoolean(false);
        var content = dialogTextKey(translationKey + "Content");
        var modal = ModalOverlay.of(translationKey + "Title", content);
        modal.addButton(ModalButton.cancel());
        modal.addButton(ModalButton.ok(() -> confirmed.set(true)));
        showAndWait(modal);
        return confirmed.get();
    }

    public static boolean confirm(String titleKey, ObservableValue<String> content) {
        var confirmed = new AtomicBoolean(false);
        var modal = ModalOverlay.of(titleKey, dialogText(content));
        modal.addButton(ModalButton.cancel());
        modal.addButton(ModalButton.ok(() -> confirmed.set(true)));
        showAndWait(modal);
        return confirmed.get();
    }
}
