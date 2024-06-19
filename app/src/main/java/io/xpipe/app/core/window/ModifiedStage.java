package io.xpipe.app.core.window;

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
        list.addListener((ListChangeListener<Window>)  c -> {
            if (c.next() && c.wasAdded()) {
                var added = c.getAddedSubList().getFirst();
                if (added instanceof Stage stage) {
                    applyStage(stage);
                }
            }
        });
    }

    private static void applyStage(Stage stage) {
        if (OsType.getLocal() != OsType.WINDOWS || AppPrefs.get() == null) {
            stage.getScene().getRoot().pseudoClassStateChanged(PseudoClass.getPseudoClass("seamless-frame"), false);
            stage.getScene().getRoot().pseudoClassStateChanged(PseudoClass.getPseudoClass("separate-frame"), true);
            return;
        }

        var ctrl = new NativeWinWindowControl(stage);
        ctrl.setWindowAttribute(DmwaWindowAttribute.DWMWA_USE_IMMERSIVE_DARK_MODE.getValue(), AppPrefs.get().theme.getValue().isDark());
        boolean backdrop;
        if (AppPrefs.get().performanceMode().get()) {
            backdrop = false;
        } else {
            backdrop = ctrl.setWindowBackdrop(DwmSystemBackDropType.MICA_ALT);
        }
        stage.getScene().getRoot().pseudoClassStateChanged(PseudoClass.getPseudoClass("seamless-frame"), backdrop);
        stage.getScene().getRoot().pseudoClassStateChanged(PseudoClass.getPseudoClass("separate-frame"), !backdrop);

        AppPrefs.get().theme.addListener((observable, oldValue, newValue) -> {
            if (!stage.isShowing()) {
                return;
            }

            Platform.runLater(() -> {
                var transition = new PauseTransition(Duration.millis(300));
                transition.setOnFinished(e -> {
                    applyStage(stage);
                    stage.setWidth(stage.getWidth() - 1);
                    Platform.runLater(() -> {
                        stage.setWidth(stage.getWidth() + 1);
                    });
                });
                transition.play();
            });
        });
    }
}
