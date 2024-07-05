package io.xpipe.app.core;

import io.xpipe.app.core.window.AppMainWindow;
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
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

import atlantafx.base.theme.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public class AppTheme {

    private static final PseudoClass LIGHT = PseudoClass.getPseudoClass("light");
    private static final PseudoClass DARK = PseudoClass.getPseudoClass("dark");
    private static final PseudoClass PRETTY = PseudoClass.getPseudoClass("pretty");
    private static final PseudoClass PERFORMANCE = PseudoClass.getPseudoClass("performance");
    private static boolean init;

    public static void initThemeHandlers(Stage stage) {
        Runnable r = () -> {
            if (AppPrefs.get() == null) {
                var def = Theme.getDefaultLightTheme();
                stage.getScene().getRoot().getStyleClass().add(def.getCssId());
                stage.getScene().getRoot().pseudoClassStateChanged(LIGHT, true);
                stage.getScene().getRoot().pseudoClassStateChanged(DARK, false);
                return;
            }

            AppPrefs.get().theme.subscribe(t -> {
                Theme.ALL.forEach(
                        theme -> stage.getScene().getRoot().getStyleClass().remove(theme.getCssId()));
                if (t == null) {
                    return;
                }

                stage.getScene().getRoot().getStyleClass().add(t.getCssId());
                stage.getScene().getStylesheets().removeAll(t.getAdditionalStylesheets());
                stage.getScene().getStylesheets().addAll(t.getAdditionalStylesheets());
                stage.getScene().getRoot().pseudoClassStateChanged(LIGHT, !t.isDark());
                stage.getScene().getRoot().pseudoClassStateChanged(DARK, t.isDark());
            });

            AppPrefs.get().performanceMode().subscribe(val -> {
                stage.getScene().getRoot().pseudoClassStateChanged(PRETTY, !val);
                stage.getScene().getRoot().pseudoClassStateChanged(PERFORMANCE, val);
            });
        };
        if (stage.getOwner() != null) {
            // If we set the theme pseudo classes earlier when the window is not shown
            // they do not apply. Is this a bug in JavaFX?
            Platform.runLater(r);
        } else {
            r.run();
        }
    }

    public static void init() {
        if (init) {
            return;
        }

        if (AppPrefs.get() == null) {
            Theme.getDefaultLightTheme().apply();
            return;
        }

        try {
            if (AppPrefs.get().theme.getValue() == null) {
                setDefault();
            }

            Platform.getPreferences().colorSchemeProperty().addListener((observableValue, colorScheme, t1) -> {
                Platform.runLater(() -> {
                    if (t1 == ColorScheme.DARK
                            && !AppPrefs.get().theme.getValue().isDark()) {
                        AppPrefs.get().theme.setValue(Theme.getDefaultDarkTheme());
                    }

                    if (t1 != ColorScheme.DARK
                            && AppPrefs.get().theme.getValue().isDark()) {
                        AppPrefs.get().theme.setValue(Theme.getDefaultLightTheme());
                    }
                });
            });
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).omit().handle();
        }

        var t = AppPrefs.get().theme.getValue();
        t.apply();
        TrackEvent.debug("Set theme " + t.getId() + " for scene");

        AppPrefs.get().theme.addListener((c, o, n) -> {
            changeTheme(n);
        });

        init = true;
    }

    private static void setDefault() {
        try {
            var colorScheme = Platform.getPreferences().getColorScheme();
            if (colorScheme == ColorScheme.DARK) {
                AppPrefs.get().theme.setValue(Theme.getDefaultDarkTheme());
            } else {
                AppPrefs.get().theme.setValue(Theme.getDefaultLightTheme());
            }
        } catch (Exception ex) {
            // The color scheme query can fail if the toolkit is not initialized properly
            AppPrefs.get().theme.setValue(Theme.getDefaultLightTheme());
        }
    }

    private static void changeTheme(Theme newTheme) {
        if (newTheme == null) {
            return;
        }

        PlatformThread.runLaterIfNeeded(() -> {
            if (AppMainWindow.getInstance() == null) {
                return;
            }

            var window = AppMainWindow.getInstance().getStage();
            var scene = window.getScene();
            Pane root = (Pane) scene.getRoot();
            Image snapshot = scene.snapshot(null);
            ImageView imageView = new ImageView(snapshot);
            root.getChildren().add(imageView);

            newTheme.apply();
            TrackEvent.debug("Set theme " + newTheme.getId() + " for scene");

            Platform.runLater(() -> {
                // Animate!
                var transition = new Timeline(
                        new KeyFrame(
                                Duration.millis(0),
                                new KeyValue(imageView.opacityProperty(), 1, Interpolator.EASE_OUT)),
                        new KeyFrame(
                                Duration.millis(600),
                                new KeyValue(imageView.opacityProperty(), 0, Interpolator.EASE_OUT)));
                transition.setOnFinished(e -> {
                    root.getChildren().remove(imageView);
                });
                transition.play();
            });
        });
    }

    public static class DerivedTheme extends Theme {

        private final String name;

        public DerivedTheme(String id, String cssId, String name, atlantafx.base.theme.Theme theme) {
            super(id, cssId, theme);
            this.name = name;
        }

        @Override
        @SneakyThrows
        public void apply() {
            var builder = new StringBuilder();
            AppResources.with(AppResources.XPIPE_MODULE, "theme/" + id + ".css", path -> {
                var content = Files.readString(path);
                builder.append(content);
            });

            // Watch out for the leading slash
            AppResources.with("atlantafx.base", theme.getUserAgentStylesheet().substring(1), path -> {
                var baseStyleContent = Files.readString(path);
                builder.append("\n")
                        .append(baseStyleContent
                                .lines()
                                .skip(builder.toString().lines().count())
                                .collect(Collectors.joining("\n")));
            });

            Application.setUserAgentStylesheet(Styles.toDataURI(builder.toString()));
        }


        public List<String> getAdditionalStylesheets() {
            return List.of();
        }

        @Override
        public ObservableValue<String> toTranslatedString() {
            return new SimpleStringProperty(name);
        }
    }

    @AllArgsConstructor
    public static class Theme implements PrefsChoiceValue {

        public static final Theme PRIMER_LIGHT = new Theme("light", "primer", new PrimerLight());
        public static final Theme PRIMER_DARK = new Theme("dark", "primer", new PrimerDark());
        public static final Theme NORD_LIGHT = new Theme("nordLight", "nord", new NordLight());
        public static final Theme NORD_DARK = new Theme("nordDark", "nord", new NordDark());
        public static final Theme CUPERTINO_LIGHT = new Theme("cupertinoLight", "cupertino", new CupertinoLight());
        public static final Theme CUPERTINO_DARK = new Theme("cupertinoDark", "cupertino", new CupertinoDark());
        public static final Theme DRACULA = new Theme("dracula", "dracula", new Dracula());
        public static final Theme MOCHA = new DerivedTheme("mocha", "primer", "Mocha", new PrimerDark());

        // Adjust this to create your own theme
        public static final Theme CUSTOM = new DerivedTheme("custom", "primer", "Custom", new PrimerDark());

        // Also include your custom theme here
        public static final List<Theme> ALL = List.of(
                PRIMER_LIGHT, PRIMER_DARK, NORD_LIGHT, NORD_DARK, CUPERTINO_LIGHT, CUPERTINO_DARK, DRACULA, MOCHA);
        protected final String id;

        @Getter
        protected final String cssId;

        protected final atlantafx.base.theme.Theme theme;

        static Theme getDefaultLightTheme() {
            return switch (OsType.getLocal()) {
                case OsType.Windows windows -> PRIMER_LIGHT;
                case OsType.Linux linux -> PRIMER_LIGHT;
                case OsType.MacOs macOs -> CUPERTINO_LIGHT;
            };
        }

        static Theme getDefaultDarkTheme() {
            return switch (OsType.getLocal()) {
                case OsType.Windows windows -> PRIMER_DARK;
                case OsType.Linux linux -> PRIMER_DARK;
                case OsType.MacOs macOs -> CUPERTINO_DARK;
            };
        }

        public boolean isDark() {
            return theme.isDarkMode();
        }

        public void apply() {
            Application.setUserAgentStylesheet(theme.getUserAgentStylesheetBSS());
        }

        public List<String> getAdditionalStylesheets() {
            var r = AppResources.getResourceURL(AppResources.XPIPE_MODULE, "theme/" + getId() + ".css");
            if (r.isEmpty()) {
                return List.of();
            } else {
                return List.of(r.get().toString());
            }
        }

        @Override
        public ObservableValue<String> toTranslatedString() {
            return new SimpleStringProperty(theme.getName());
        }

        @Override
        public String getId() {
            return id;
        }
    }
}
