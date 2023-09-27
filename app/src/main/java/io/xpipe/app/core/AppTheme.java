package io.xpipe.app.core;

import atlantafx.base.theme.*;
import com.jthemedetecor.OsThemeDetector;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.OsType;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.css.PseudoClass;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class AppTheme {

    private static final PseudoClass LIGHT = PseudoClass.getPseudoClass("light");
    private static final PseudoClass DARK = PseudoClass.getPseudoClass("dark");
    private static final PseudoClass PRETTY = PseudoClass.getPseudoClass("pretty");
    private static final PseudoClass PERFORMANCE = PseudoClass.getPseudoClass("performance");

    public static void initTheme(Window stage) {
        var t = AppPrefs.get().theme.getValue();
        if (t == null) {
            return;
        }

        stage.getScene().getRoot().pseudoClassStateChanged(LIGHT, !t.getTheme().isDarkMode());
        stage.getScene().getRoot().pseudoClassStateChanged(DARK, t.getTheme().isDarkMode());
        SimpleChangeListener.apply(AppPrefs.get().performanceMode(),val -> {
            stage.getScene().getRoot().pseudoClassStateChanged(PRETTY, !val);
            stage.getScene().getRoot().pseudoClassStateChanged(PERFORMANCE, val);
        });

        var transparent = AppPrefs.get().enableWindowTransparency().getValue();
        stage.setOpacity(transparent ? t.getTransparencyOpacity() : 1.0);
    }

    public static void init() {
        if (AppPrefs.get() == null) {
            Application.setUserAgentStylesheet(Theme.getDefaultLightTheme().getTheme().getUserAgentStylesheet());
            return;
        }

        OsThemeDetector detector = OsThemeDetector.getDetector();
        if (AppPrefs.get().theme.getValue() == null) {
            try {
                setDefault(detector.isDark());
            } catch (Throwable ex) {
                ErrorEvent.fromThrowable(ex).omit().handle();
                setDefault(false);
            }
        }
        var t = AppPrefs.get().theme.getValue();

        Application.setUserAgentStylesheet(t.getTheme().getUserAgentStylesheet());
        TrackEvent.debug("Set theme " + t.getId() + " for scene");

        detector.registerListener(dark -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (dark && !AppPrefs.get().theme.getValue().getTheme().isDarkMode()) {
                    AppPrefs.get().theme.setValue(Theme.getDefaultDarkTheme());
                }

                if (!dark && AppPrefs.get().theme.getValue().getTheme().isDarkMode()) {
                    AppPrefs.get().theme.setValue(Theme.getDefaultLightTheme());
                }
            });
        });

        AppPrefs.get().theme.addListener((c, o, n) -> {
            changeTheme(n);
        });

        AppPrefs.get().enableWindowTransparency().addListener((observable, oldValue, newValue) -> {
            var th = AppPrefs.get().theme;
            PlatformThread.runLaterIfNeeded(() -> {
                for (Window window : Window.getWindows()) {
                    window.setOpacity(newValue ? th.get().getTransparencyOpacity() : 1.0);
                }
            });
        });
    }

    private static void setDefault(boolean dark) {
        if (dark) {
            AppPrefs.get().theme.setValue(Theme.getDefaultDarkTheme());
        } else {
            AppPrefs.get().theme.setValue(Theme.getDefaultLightTheme());
        }
    }

    private static void changeTheme(Theme newTheme) {
        if (newTheme == null) {
            return;
        }

        PlatformThread.runLaterIfNeeded(() -> {
            for (Window window : Window.getWindows()) {
                var scene = window.getScene();
                Image snapshot = scene.snapshot(null);
                initTheme(window);
                Pane root = (Pane) scene.getRoot();

                ImageView imageView = new ImageView(snapshot);
                root.getChildren().add(imageView);

                // Animate!
                var transition = new Timeline(
                        new KeyFrame(
                                Duration.ZERO, new KeyValue(imageView.opacityProperty(), 1, Interpolator.EASE_OUT)),
                        new KeyFrame(
                                Duration.millis(1250),
                                new KeyValue(imageView.opacityProperty(), 0, Interpolator.EASE_OUT)));
                transition.setOnFinished(e -> {
                    root.getChildren().remove(imageView);
                });
                transition.play();
            }

            Application.setUserAgentStylesheet(newTheme.getTheme().getUserAgentStylesheet());
            TrackEvent.debug("Set theme " + newTheme.getId() + " for scene");
        });
    }

    @AllArgsConstructor
    @Getter
    public enum Theme implements PrefsChoiceValue {
        PRIMER_LIGHT("light", new PrimerLight(), 0.92),
        PRIMER_DARK("dark", new PrimerDark(), 0.92),
        NORD_LIGHT("nordLight", new NordLight(), 0.92),
        NORD_DARK("nordDark", new NordDark(), 0.92),
        CUPERTINO_LIGHT("cupertinoLight", new CupertinoLight(), 0.92),
        CUPERTINO_DARK("cupertinoDark", new CupertinoDark(), 0.92),
        DRACULA("dracula", new Dracula(), 0.92);

        static Theme getDefaultLightTheme() {
            return switch (OsType.getLocal()) {
                case OsType.Windows windows -> PRIMER_LIGHT;
                case OsType.Linux linux -> NORD_LIGHT;
                case OsType.MacOs macOs -> CUPERTINO_LIGHT;
            };
        }

        static Theme getDefaultDarkTheme() {
            return switch (OsType.getLocal()) {
                case OsType.Windows windows -> PRIMER_DARK;
                case OsType.Linux linux -> NORD_DARK;
                case OsType.MacOs macOs -> CUPERTINO_DARK;
            };
        }

        private final String id;
        private final atlantafx.base.theme.Theme theme;
        private final double transparencyOpacity;

        @Override
        public String toTranslatedString() {
            return theme.getName();
        }
    }
}
