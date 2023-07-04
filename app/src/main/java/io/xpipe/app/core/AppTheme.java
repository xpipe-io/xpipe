package io.xpipe.app.core;

import atlantafx.base.theme.*;
import com.jthemedetecor.OsThemeDetector;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.fxcomps.util.PlatformThread;
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
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class AppTheme {

    public record AccentColor(Color primaryColor, PseudoClass pseudoClass) {

        public static AccentColor xpipeBlue() {
            return new AccentColor(Color.web("#11B4B4"), PseudoClass.getPseudoClass("accent-primer-purple"));
        }
    }

    public static void init() {
        if (AppPrefs.get() == null) {
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
        PRIMER_LIGHT("light", new PrimerLight()),
        PRIMER_DARK("dark", new PrimerDark()),
        NORD_LIGHT("nordLight", new NordLight()),
        NORD_DARK("nordDark", new NordDark()),
        CUPERTINO_LIGHT("cupertinoLight", new CupertinoLight()),
        CUPERTINO_DARK("cupertinoDark", new CupertinoDark()),
        DRACULA("dracula", new Dracula());

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

        @Override
        public String toTranslatedString() {
            return theme.getName();
        }
    }
}
