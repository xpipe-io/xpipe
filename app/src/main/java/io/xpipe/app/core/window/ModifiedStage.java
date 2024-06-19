package io.xpipe.app.core.window;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.OsType;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.SneakyThrows;

public class ModifiedStage extends Stage {

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static void hook() {
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
        if (OsType.getLocal() != OsType.WINDOWS) {
            return;
        }

        var ctrl = new NativeWinWindowControl(stage);
        ctrl.setWindowAttribute(DmwaWindowAttribute.DWMWA_USE_IMMERSIVE_DARK_MODE.getValue(), AppPrefs.get().theme.getValue().isDark());
        var backdrop = ctrl.setWindowBackdrop(DwmSystemBackDropType.MICA_ALT);
        stage.getScene().getRoot().pseudoClassStateChanged(PseudoClass.getPseudoClass("seamless-frame"), backdrop);
        stage.getScene().getRoot().pseudoClassStateChanged(PseudoClass.getPseudoClass("separate-frame"), !backdrop);
    }
}
