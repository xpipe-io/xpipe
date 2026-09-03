package io.xpipe.app.core.window;

import io.xpipe.app.util.OsType;

import javafx.stage.Stage;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppWindowBounds {

    public static void fixInvalidStagePosition(Stage stage) {
        if (OsType.ofLocal() != OsType.LINUX) {
            return;
        }

        var xSet = new AtomicBoolean();
        stage.xProperty().addListener((observable, oldValue, newValue) -> {
            var n = newValue.doubleValue();
            var o = oldValue.doubleValue();
            if (stage.isShowing() && areNumbersValid(o, n)) {
                // Ignore rounding events
                if (Math.abs(n - o) <= 0.5) {
                    return;
                }

                if (!xSet.getAndSet(true) && !stage.isMaximized() && n <= 0.0 && o > 0.0 && Math.abs(n - o) > 100) {
                    stage.setX(o);
                }
            }
        });

        var ySet = new AtomicBoolean();
        stage.yProperty().addListener((observable, oldValue, newValue) -> {
            var n = newValue.doubleValue();
            var o = oldValue.doubleValue();
            if (stage.isShowing() && areNumbersValid(o, n)) {
                // Ignore rounding events
                if (Math.abs(n - o) <= 0.5) {
                    return;
                }

                if (!ySet.getAndSet(true) && !stage.isMaximized() && n <= 0.0 && o > 0.0 && Math.abs(n - o) > 20) {
                    stage.setY(o);
                }
            }
        });
    }

    private static boolean areNumbersValid(double... args) {
        return Arrays.stream(args).allMatch(Double::isFinite);
    }
}
