package io.xpipe.app.core.window;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.core.process.OsType;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import org.apache.commons.lang3.SystemUtils;

public class ModifiedStage extends Stage {

    public static boolean mergeFrame() {
        return SystemUtils.IS_OS_WINDOWS_11 || SystemUtils.IS_OS_MAC;
    }

    public static void init() {
        ObservableList<Window> list = Window.getWindows();
        list.addListener((ListChangeListener<Window>) c -> {
            if (c.next() && c.wasAdded()) {
                var added = c.getAddedSubList().getFirst();
                if (added instanceof Stage stage) {
                    hookUpStage(stage);
                }
            }
        });
    }

    public static void prepareStage(Stage stage) {
        if (mergeFrame()) {
            stage.initStyle(StageStyle.UNIFIED);
        }
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

        var applyToStage = (OsType.getLocal() == OsType.WINDOWS)
                || (OsType.getLocal() == OsType.MACOS
                        && AppMainWindow.getInstance() != null
                        && AppMainWindow.getInstance().getStage() == stage);
        if (!applyToStage || AppPrefs.get() == null || AppPrefs.get().theme.getValue() == null) {
            stage.getScene().getRoot().pseudoClassStateChanged(PseudoClass.getPseudoClass("seamless-frame"), false);
            stage.getScene().getRoot().pseudoClassStateChanged(PseudoClass.getPseudoClass("separate-frame"), true);
            return;
        }

        switch (OsType.getLocal()) {
            case OsType.Linux linux -> {}
            case OsType.MacOs macOs -> {
                var ctrl = new NativeMacOsWindowControl(stage);
                var seamlessFrame = !AppPrefs.get().performanceMode().get() && mergeFrame();
                var seamlessFrameApplied = ctrl.setAppearance(
                                seamlessFrame, AppPrefs.get().theme.getValue().isDark())
                        && seamlessFrame;
                stage.getScene()
                        .getRoot()
                        .pseudoClassStateChanged(PseudoClass.getPseudoClass("seamless-frame"), seamlessFrameApplied);
                stage.getScene()
                        .getRoot()
                        .pseudoClassStateChanged(PseudoClass.getPseudoClass("separate-frame"), !seamlessFrameApplied);
            }
            case OsType.Windows windows -> {
                var ctrl = new NativeWinWindowControl(stage);
                ctrl.setWindowAttribute(
                        NativeWinWindowControl.DmwaWindowAttribute.DWMWA_USE_IMMERSIVE_DARK_MODE.get(),
                        AppPrefs.get().theme.getValue().isDark());
                boolean seamlessFrame;
                if (AppPrefs.get().performanceMode().get() || !mergeFrame()) {
                    seamlessFrame = false;
                } else {
                    seamlessFrame = ctrl.setWindowBackdrop(NativeWinWindowControl.DwmSystemBackDropType.MICA_ALT);
                }
                stage.getScene()
                        .getRoot()
                        .pseudoClassStateChanged(PseudoClass.getPseudoClass("seamless-frame"), seamlessFrame);
                stage.getScene()
                        .getRoot()
                        .pseudoClassStateChanged(PseudoClass.getPseudoClass("separate-frame"), !seamlessFrame);
            }
        }
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
