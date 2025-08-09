package io.xpipe.app.core;

import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ColorHelper;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.core.OsType;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import atlantafx.base.theme.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AppTheme {

    private static final PseudoClass LIGHT = PseudoClass.getPseudoClass("light");
    private static final PseudoClass DARK = PseudoClass.getPseudoClass("dark");
    private static final PseudoClass PRETTY = PseudoClass.getPseudoClass("pretty");
    private static final PseudoClass PERFORMANCE = PseudoClass.getPseudoClass("performance");
    private static boolean init;

    public static void initThemeHandlers(Stage stage) {
        stage.getScene().rootProperty().subscribe(root -> {
            if (root == null) {
                return;
            }

            root.pseudoClassStateChanged(
                    PseudoClass.getPseudoClass(OsType.getLocal().getId()), true);
            if (AppPrefs.get() == null) {
                var def = Theme.getDefaultLightTheme();
                root.pseudoClassStateChanged(PseudoClass.getPseudoClass(def.getCssId()), true);
                root.pseudoClassStateChanged(LIGHT, true);
                root.pseudoClassStateChanged(DARK, false);
                root.pseudoClassStateChanged(PRETTY, true);
                root.pseudoClassStateChanged(PERFORMANCE, false);
                return;
            }

            AppPrefs.get().theme().subscribe(t -> {
                Theme.ALL.forEach(theme -> {
                    root.pseudoClassStateChanged(
                            PseudoClass.getPseudoClass(theme.getCssId()),
                            theme.getCssId().equals(t.getCssId()));
                });
                if (t == null) {
                    return;
                }

                root.pseudoClassStateChanged(LIGHT, !t.isDark());
                root.pseudoClassStateChanged(DARK, t.isDark());
            });

            AppPrefs.get().performanceMode().subscribe(val -> {
                root.pseudoClassStateChanged(PRETTY, !val);
                root.pseudoClassStateChanged(PERFORMANCE, val);
            });
        });
    }

    public static void init() {
        if (init) {
            TrackEvent.trace("Theme init requested again");
            return;
        }

        if (AppPrefs.get() == null) {
            TrackEvent.trace("Theme init prior to prefs init, setting theme to default");
            Theme.getDefaultLightTheme().apply();
            return;
        }

        try {
            var lastSystemDark = AppCache.getBoolean("lastDarkTheme", false);
            var nowDark = isDarkMode();
            AppCache.update("lastDarkTheme", nowDark);
            if (AppPrefs.get().theme().getValue() == null || lastSystemDark != nowDark) {
                TrackEvent.trace("Updating theme to system theme");
                setDefault();
            }

            Platform.getPreferences().addListener((MapChangeListener<? super String, ? super Object>) change -> {
                TrackEvent.withTrace("Platform preference changed")
                        .tag("change", change.toString())
                        .handle();
            });

            Platform.getPreferences().addListener((MapChangeListener<? super String, ? super Object>) change -> {
                if (change.getKey().equals("GTK.theme_name")) {
                    Platform.runLater(() -> {
                        updateThemeToThemeName(change.getValueRemoved(), change.getValueAdded());
                    });
                }
            });

            Platform.getPreferences().colorSchemeProperty().addListener((observableValue, colorScheme, t1) -> {
                Platform.runLater(() -> {
                    updateThemeToColorScheme(t1);
                });
            });
        } catch (IllegalStateException ex) {
            // The platform preferences are sometimes not initialized yet
            ErrorEventFactory.fromThrowable(ex).expected().omit().handle();
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).omit().handle();
        }

        var t = AppPrefs.get().theme().getValue();
        t.apply();
        TrackEvent.debug("Set theme " + t.getId() + " for scene");

        AppPrefs.get().theme().addListener((c, o, n) -> {
            changeTheme(n);
        });

        init = true;
    }

    private static void updateThemeToThemeName(Object oldName, Object newName) {
        if (OsType.getLocal() == OsType.LINUX && newName != null) {
            var toDark = (oldName == null || !oldName.toString().contains("-dark"))
                    && newName.toString().contains("-dark");
            var toLight = (oldName == null || oldName.toString().contains("-dark"))
                    && !newName.toString().contains("-dark");
            if (toDark) {
                updateThemeToColorScheme(ColorScheme.DARK);
            } else if (toLight) {
                updateThemeToColorScheme(ColorScheme.LIGHT);
            }
        }
    }

    private static boolean isDarkMode() {
        var nowDark = Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
        if (nowDark) {
            return true;
        }

        var gtkTheme = Platform.getPreferences().get("GTK.theme_name");
        return gtkTheme != null && gtkTheme.toString().contains("-dark");
    }

    private static void updateThemeToColorScheme(ColorScheme colorScheme) {
        if (colorScheme == null) {
            return;
        }

        if (colorScheme == ColorScheme.DARK
                && !AppPrefs.get().theme().getValue().isDark()) {
            AppPrefs.get().theme.setValue(Theme.getDefaultDarkTheme());
        }

        if (colorScheme != ColorScheme.DARK && AppPrefs.get().theme().getValue().isDark()) {
            AppPrefs.get().theme.setValue(Theme.getDefaultLightTheme());
        }
    }

    public static void reset() {
        if (!init) {
            return;
        }

        PlatformThread.runLaterIfNeededBlocking(() -> {
            var nowDark = isDarkMode();
            AppCache.update("lastDarkTheme", nowDark);
        });
    }

    private static void setDefault() {
        try {
            var colorScheme = Platform.getPreferences().getColorScheme();
            if (colorScheme == ColorScheme.DARK) {
                AppPrefs.get().theme.setValue(Theme.getDefaultDarkTheme());
            } else {
                AppPrefs.get().theme.setValue(Theme.getDefaultLightTheme());
            }
        } catch (IllegalStateException ex) {
            // The platform preferences are sometimes not initialized yet
            ErrorEventFactory.fromThrowable(ex).expected().omit().handle();
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
            var window = AppMainWindow.getInstance();
            if (window == null) {
                return;
            }

            TrackEvent.debug("Setting theme " + newTheme.getId() + " for scene");

            // Don't animate transition in performance mode
            if (AppPrefs.get() == null || AppPrefs.get().performanceMode().get()) {
                newTheme.apply();
                return;
            }

            var stage = window.getStage();
            var scene = stage.getScene();
            Pane root = (Pane) scene.getRoot();
            Image snapshot = scene.snapshot(null);
            ImageView imageView = new ImageView(snapshot);
            root.getChildren().add(imageView);
            newTheme.apply();

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
        private final int skipLines;

        public DerivedTheme(
                String id,
                String cssId,
                String name,
                atlantafx.base.theme.Theme theme,
                AppFontSizes sizes,
                Color baseColor,
                Color borderColor,
                Supplier<Color> contextMenuColor,
                int displayBorderRadius,
                int skipLines) {
            super(id, cssId, theme, sizes, baseColor, borderColor, contextMenuColor, displayBorderRadius);
            this.name = name;
            this.skipLines = skipLines;
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
                        .append(baseStyleContent.lines().skip(skipLines).collect(Collectors.joining("\n")));
            });

            Application.setUserAgentStylesheet(Styles.toDataURI(builder.toString()));
        }

        @Override
        public ObservableValue<String> toTranslatedString() {
            return new SimpleStringProperty(name);
        }
    }

    @AllArgsConstructor
    public static class Theme implements PrefsChoiceValue {

        public static final Theme PRIMER_LIGHT = new Theme(
                "light",
                "primer",
                new PrimerLight(),
                AppFontSizes.forOs(AppFontSizes.BASE_10_5, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11),
                Color.WHITE,
                Color.web("#24292f"),
                () -> ColorHelper.withOpacity(
                        Platform.getPreferences()
                                .getAccentColor()
                                .darker()
                                .desaturate()
                                .brighter(),
                        0.3),
                4);
        public static final Theme PRIMER_DARK = new Theme(
                "dark",
                "primer",
                new PrimerDark(),
                AppFontSizes.forOs(AppFontSizes.BASE_11, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11),
                Color.web("#0d1117"),
                Color.web("#c9d1d9"),
                () -> ColorHelper.withOpacity(
                        Platform.getPreferences()
                                .getAccentColor()
                                .desaturate()
                                .desaturate()
                                .darker(),
                        0.2),
                4);
        public static final Theme NORD_LIGHT = new Theme(
                "nordLight",
                "nord",
                new NordLight(),
                AppFontSizes.forOs(AppFontSizes.BASE_10_5, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11),
                Color.web("#dadadc"),
                Color.web("#2E3440"),
                () -> ColorHelper.withOpacity(
                        Platform.getPreferences()
                                .getAccentColor()
                                .darker()
                                .desaturate()
                                .brighter(),
                        0.3),
                0);
        public static final Theme NORD_DARK = new Theme(
                "nordDark",
                "nord",
                new NordDark(),
                AppFontSizes.forOs(AppFontSizes.BASE_11, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11),
                Color.web("#2d3137"),
                Color.web("#24292f"),
                () -> ColorHelper.withOpacity(
                        Platform.getPreferences()
                                .getAccentColor()
                                .desaturate()
                                .desaturate()
                                .darker(),
                        0.2),
                0);
        public static final Theme CUPERTINO_LIGHT = new Theme(
                "cupertinoLight",
                "cupertino",
                new CupertinoLight(),
                AppFontSizes.forOs(AppFontSizes.BASE_10_5, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11),
                Color.WHITE,
                Color.BLACK,
                () -> ColorHelper.withOpacity(
                        Platform.getPreferences()
                                .getAccentColor()
                                .darker()
                                .desaturate()
                                .brighter(),
                        0.3),
                4);
        public static final Theme CUPERTINO_DARK = new Theme(
                "cupertinoDark",
                "cupertino",
                new CupertinoDark(),
                AppFontSizes.forOs(AppFontSizes.BASE_11, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11),
                Color.BLACK,
                Color.WHITE,
                () -> ColorHelper.withOpacity(
                        Platform.getPreferences()
                                .getAccentColor()
                                .desaturate()
                                .desaturate()
                                .darker(),
                        0.2),
                4);
        public static final Theme DRACULA = new Theme(
                "dracula",
                "dracula",
                new Dracula(),
                AppFontSizes.forOs(AppFontSizes.BASE_11, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11),
                Color.web("#383f49"),
                Color.web("#9580ff"),
                () -> ColorHelper.withOpacity(
                        Platform.getPreferences()
                                .getAccentColor()
                                .desaturate()
                                .desaturate()
                                .darker(),
                        0.2),
                6);
        public static final Theme MOCHA = new DerivedTheme(
                "mocha",
                "mocha",
                "Mocha",
                new PrimerDark(),
                AppFontSizes.forOs(AppFontSizes.BASE_11, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11),
                Color.web("#2E2E4EFF"),
                Color.web("#CDD6F4FF"),
                () -> ColorHelper.withOpacity(
                        Platform.getPreferences()
                                .getAccentColor()
                                .desaturate()
                                .desaturate()
                                .darker(),
                        0.2),
                4,
                91);

        // Adjust this to create your own theme
        public static final Theme CUSTOM = new DerivedTheme(
                "custom",
                "primer",
                "Custom",
                new PrimerDark(),
                AppFontSizes.forOs(AppFontSizes.BASE_10_5, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11),
                Color.web("#0d1117"),
                Color.web("#24292f"),
                () -> ColorHelper.withOpacity(
                        Platform.getPreferences().getAccentColor().desaturate().desaturate(), 0.2),
                4,
                91);

        // Also include your custom theme here
        public static final List<Theme> ALL = List.of(
                PRIMER_LIGHT, PRIMER_DARK, NORD_LIGHT, NORD_DARK, CUPERTINO_LIGHT, CUPERTINO_DARK, DRACULA, MOCHA);
        protected final String id;

        @Getter
        protected final String cssId;

        protected final atlantafx.base.theme.Theme theme;

        @Getter
        protected final AppFontSizes fontSizes;

        @Getter
        protected final Color baseColor;

        @Getter
        protected final Color borderColor;

        @Getter
        protected final Supplier<Color> contextMenuColor;

        @Getter
        protected final int displayBorderRadius;

        static Theme getDefaultLightTheme() {
            return switch (OsType.getLocal()) {
                case OsType.Windows ignored -> PRIMER_LIGHT;
                case OsType.Linux ignored -> PRIMER_LIGHT;
                case OsType.MacOs ignored -> CUPERTINO_LIGHT;
            };
        }

        static Theme getDefaultDarkTheme() {
            return switch (OsType.getLocal()) {
                case OsType.Windows ignored -> PRIMER_DARK;
                case OsType.Linux ignored -> PRIMER_DARK;
                case OsType.MacOs ignored -> CUPERTINO_DARK;
            };
        }

        public boolean isDark() {
            return theme.isDarkMode();
        }

        public void apply() {
            Application.setUserAgentStylesheet(theme.getUserAgentStylesheetBSS());
        }

        protected String getPlatformPreferencesStylesheet() {
            var c = contextMenuColor.get();
            var hex = ColorHelper.toWeb(c);
            var s = "* { -color-context-menu: " + hex + "; }";
            return s;
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
