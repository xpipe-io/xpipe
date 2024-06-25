package io.xpipe.app.core.window;

import io.xpipe.app.core.App;
import io.xpipe.core.process.OsType;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppWindowBounds {

    public static void fixInvalidStagePosition(Stage stage) {
        if (OsType.getLocal() != OsType.LINUX) {
            return;
        }

        var xSet = new AtomicBoolean();
        stage.xProperty().addListener((observable, oldValue, newValue) -> {
            var n = newValue.doubleValue();
            var o = oldValue.doubleValue();
            if (stage.isShowing() && areNumbersValid(o, n)) {
                // Ignore rounding events
                if (Math.abs(n - o) < 0.5) {
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
                if (Math.abs(n - o) < 0.5) {
                    return;
                }

                if (!ySet.getAndSet(true) && !stage.isMaximized() && n <= 0.0 && o > 0.0 && Math.abs(n - o) > 20) {
                    stage.setY(o);
                }
            }
        });
    }

    public static Stage centerStage() {
        var stage = new Stage() {
            @Override
            public void centerOnScreen() {
                if (App.getApp() == null) {
                    super.centerOnScreen();
                    return;
                }

                var stage = App.getApp().getStage();
                this.setX(stage.getX() + stage.getWidth() / 2 - this.getWidth() / 2);
                this.setY(stage.getY() + stage.getHeight() / 2 - this.getHeight() / 2);
                clampWindow(this).ifPresent(rectangle2D -> {
                    this.setX(rectangle2D.getMinX());
                    this.setY(rectangle2D.getMinY());
                    this.setWidth(rectangle2D.getWidth());
                    this.setHeight(rectangle2D.getHeight());
                });
            }
        };
        return stage;
    }

    public static Optional<Rectangle2D> clampWindow(Stage stage) {
        if (!areNumbersValid(stage.getWidth(), stage.getHeight())) {
            return Optional.empty();
        }

        var allScreenBounds = computeWindowScreenBounds(stage);
        if (!areNumbersValid(
                allScreenBounds.getMinX(),
                allScreenBounds.getMinY(),
                allScreenBounds.getMaxX(),
                allScreenBounds.getMaxY())) {
            return Optional.empty();
        }

        // Alerts do not have a custom x/y set, but we are able to handle that

        boolean changed = false;

        double x = 0;
        if (areNumbersValid(stage.getX())) {
            x = stage.getX();
            if (x < allScreenBounds.getMinX()) {
                x = allScreenBounds.getMinX();
                changed = true;
            }
        }

        double y = 0;
        if (areNumbersValid(stage.getY())) {
            y = stage.getY();
            if (y < allScreenBounds.getMinY()) {
                y = allScreenBounds.getMinY();
                changed = true;
            }
        }

        double w = stage.getWidth();
        double h = stage.getHeight();
        if (x + w > allScreenBounds.getMaxX()) {
            w = allScreenBounds.getMaxX() - x;
            changed = true;
        }
        if (y + h > allScreenBounds.getMaxY()) {
            h = allScreenBounds.getMaxY() - y;
            changed = true;
        }

        // This should not happen but on weird Linux systems nothing is impossible
        if (w < 0 || h < 0) {
            return Optional.empty();
        }

        return changed ? Optional.of(new Rectangle2D(x, y, w, h)) : Optional.empty();
    }

    private static boolean areNumbersValid(double... args) {
        return Arrays.stream(args).allMatch(Double::isFinite);
    }

    private static List<Screen> getWindowScreens(Stage stage) {
        if (!areNumbersValid(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())) {
            return stage.getOwner() != null && stage.getOwner() instanceof Stage ownerStage
                    ? getWindowScreens(ownerStage)
                    : List.of(Screen.getPrimary());
        }

        return Screen.getScreensForRectangle(
                new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()));
    }

    private static Rectangle2D computeWindowScreenBounds(Stage stage) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Screen screen : getWindowScreens(stage)) {
            Rectangle2D screenBounds = screen.getBounds();
            if (screenBounds.getMinX() < minX) {
                minX = screenBounds.getMinX();
            }
            if (screenBounds.getMinY() < minY) {
                minY = screenBounds.getMinY();
            }
            if (screenBounds.getMaxX() > maxX) {
                maxX = screenBounds.getMaxX();
            }
            if (screenBounds.getMaxY() > maxY) {
                maxY = screenBounds.getMaxY();
            }
        }
        // Taskbar adjustment
        maxY -= 50;

        var w = maxX - minX;
        var h = maxY - minY;

        // This should not happen but on weird Linux systems nothing is impossible
        if (w < 0 || h < 0) {
            return new Rectangle2D(0, 0, 800, 600);
        }

        return new Rectangle2D(minX, minY, w, h);
    }
}
