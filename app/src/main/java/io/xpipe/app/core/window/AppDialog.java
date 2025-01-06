package io.xpipe.app.core.window;

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
    private static final ObservableList<ModalOverlay> modalOverlay = FXCollections.observableArrayList();

    private static void showMainWindow() {
        PlatformInit.init(true);
        AppMainWindow.init(true);
    }

    public static void closeDialog(ModalOverlay overlay) {
        PlatformThread.runLaterIfNeeded(() -> {
            modalOverlay.remove(overlay);
        });
    }

    public static void waitForAllDialogsClose() {
        while (!modalOverlay.isEmpty()) {
            ThreadHelper.sleep(10);
        }
    }

    private static void waitForDialogClose(ModalOverlay overlay) {
        while (modalOverlay.contains(overlay)) {
            ThreadHelper.sleep(10);
        }
    }

    public static void show(ModalOverlay o) {
        show(o, false);
    }

    public static void showAndWait(ModalOverlay o) {
        show(o, true);
    }

    public static void show(ModalOverlay o, boolean wait) {
        showMainWindow();
        if (!Platform.isFxApplicationThread()) {
            PlatformThread.runLaterIfNeededBlocking(() -> {
                modalOverlay.add(o);
            });
            if (wait) {
                waitForDialogClose(o);
            }
            ThreadHelper.sleep(200);
        } else {
            var key = new Object();
            PlatformThread.runLaterIfNeededBlocking(() -> {
                modalOverlay.add(o);
                modalOverlay.addListener(new ListChangeListener<>() {
                    @Override
                    public void onChanged(Change<? extends ModalOverlay> c) {
                        if (!c.getList().contains(o)) {
                            var transition = new PauseTransition(Duration.millis(200));
                            transition.setOnFinished(e -> {
                                if (wait) {
                                    Platform.exitNestedEventLoop(key, null);
                                }
                            });
                            transition.play();
                            modalOverlay.removeListener(this);
                        }
                    }
                });
            });
            if (wait) {
                Platform.enterNestedEventLoop(key);
                waitForDialogClose(o);
            }
        }
    }

    public static Comp<?> dialogTextKey(String s) {
        return dialogText(AppI18n.get(s));
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
        var content = dialogTextKey(AppI18n.get(translationKey + "Content"));
        var modal = ModalOverlay.of(translationKey + "Title", content);
        modal.addButton(ModalButton.cancel());
        modal.addButton(ModalButton.ok(() -> confirmed.set(true)));
        showAndWait(modal);
        return confirmed.get();
    }
}
