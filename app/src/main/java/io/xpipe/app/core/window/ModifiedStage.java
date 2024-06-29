package io.xpipe.app.core.window;

import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.OsType;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import lombok.SneakyThrows;

public class ModifiedStage extends Stage {

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static void init() {
        var windowsField = Window.class.getDeclaredField("windows");
        windowsField.setAccessible(true);
        ObservableList<Window> list = (ObservableList<Window>) windowsField.get(null);
        list.addListener((ListChangeListener<Window>) c -> {
            if (c.next() && c.wasAdded()) {
                var added = c.getAddedSubList().getFirst();
                if (added instanceof Stage stage) {
                    hookUpStage(stage);
                }
            }
        });
    }

    private static void hookUpStage(Stage stage) {
        applyModes(stage);
        if (AppPrefs.get() != null) {
            AppPrefs.get().theme.addListener((observable, oldValue, newValue) -> {
                updateStage(stage);
            });
            AppPrefs.get().performanceMode().addListener((observable, oldValue, newValue) -> {
                updateStage(stage);
            });
        }
    }

    private static void applyModes(Stage stage) {
        if (stage.getScene() == null) {
            return;
        }

        if (OsType.getLocal() != OsType.WINDOWS || AppPrefs.get() == null || AppPrefs.get().theme.getValue() == null) {
            stage.getScene().getRoot().pseudoClassStateChanged(PseudoClass.getPseudoClass("seamless-frame"), false);
            stage.getScene().getRoot().pseudoClassStateChanged(PseudoClass.getPseudoClass("separate-frame"), true);
            return;
        }

        var ctrl = new NativeWinWindowControl(stage);
        ctrl.setWindowAttribute(
                NativeWinWindowControl.DmwaWindowAttribute.DWMWA_USE_IMMERSIVE_DARK_MODE.get(),
                AppPrefs.get().theme.getValue().isDark());
        boolean backdrop;
        if (AppPrefs.get().performanceMode().get()) {
            backdrop = false;
        } else {
            backdrop = ctrl.setWindowBackdrop(NativeWinWindowControl.DwmSystemBackDropType.MICA_ALT);
        }
        stage.getScene().getRoot().pseudoClassStateChanged(PseudoClass.getPseudoClass("seamless-frame"), backdrop);
        stage.getScene().getRoot().pseudoClassStateChanged(PseudoClass.getPseudoClass("separate-frame"), !backdrop);
    }

    private static void updateStage(Stage stage) {
        if (!stage.isShowing()) {
            return;
        }

        PlatformThread.runLaterIfNeeded(() -> {
            var transition = new PauseTransition(Duration.millis(300));
            transition.setOnFinished(e -> {
                applyModes(stage);
                stage.setWidth(stage.getWidth() - 1);
                Platform.runLater(() -> {
                    stage.setWidth(stage.getWidth() + 1);
                });
            });
            transition.play();
        });
    }
}
