package io.xpipe.app.core;

import atlantafx.base.theme.*;
import com.dlsc.atlantafx.themes.FallDark;
import com.dlsc.atlantafx.themes.FallLight;
import com.dlsc.atlantafx.themes.SpringDark;
import com.dlsc.atlantafx.themes.WinterDark;
import io.xpipe.app.platform.ColorHelper;
import io.xpipe.app.prefs.PrefsChoiceValue;
import io.xpipe.app.util.OsType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.Stylesheet;
import javafx.scene.paint.Color;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@AllArgsConstructor
public class AppTheme implements PrefsChoiceValue {

    public static final AppTheme PRIMER_LIGHT = new AppTheme("light", "primer", new PrimerLight(),
            () -> AppFontSizes.forOs(AppFontSizes.BASE_10_5, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11), Color.WHITE, Color.web("#24292f"),
            () -> ColorHelper.withOpacity(Platform.getPreferences().getAccentColor().darker().desaturate().brighter(), 0.3),
            () -> Platform.getPreferences().getAccentColor(), 4, "atlantafx.base");
    public static final AppTheme PRIMER_DARK = new AppTheme("dark", "primer", new PrimerDark(),
            () -> AppFontSizes.forOs(AppFontSizes.BASE_11, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11), Color.web("#0d1117"), Color.web("#c9d1d9"),
            () -> ColorHelper.withOpacity(Platform.getPreferences().getAccentColor().desaturate().desaturate().darker(), 0.2),
            () -> Platform.getPreferences().getAccentColor(), 4, "atlantafx.base");
    public static final AppTheme NORD_LIGHT = new AppTheme("nordLight", "nord", new NordLight(),
            () -> AppFontSizes.forOs(AppFontSizes.BASE_10_5, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11), Color.web("#dadadc"),
            Color.web("#2E3440"), () -> ColorHelper.withOpacity(Platform.getPreferences().getAccentColor().darker().desaturate().brighter(), 0.3),
            () -> Platform.getPreferences().getAccentColor(), 0, "atlantafx.base");
    public static final AppTheme NORD_DARK = new AppTheme("nordDark", "nord", new NordDark(),
            () -> AppFontSizes.forOs(AppFontSizes.BASE_11, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11), Color.web("#2d3137"), Color.web("#24292f"),
            () -> ColorHelper.withOpacity(Platform.getPreferences().getAccentColor().desaturate().desaturate().darker(), 0.2),
            () -> Platform.getPreferences().getAccentColor(), 0, "atlantafx.base");
    public static final AppTheme CUPERTINO_LIGHT = new AppTheme("cupertinoLight", "cupertino", new CupertinoLight(),
            () -> AppFontSizes.forOs(AppFontSizes.BASE_10_5, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11), Color.WHITE, Color.BLACK,
            () -> ColorHelper.withOpacity(Platform.getPreferences().getAccentColor().darker().desaturate().brighter(), 0.3),
            () -> Platform.getPreferences().getAccentColor(), 4, "atlantafx.base");
    public static final AppTheme CUPERTINO_DARK = new AppTheme("cupertinoDark", "cupertino", new CupertinoDark(),
            () -> AppFontSizes.forOs(AppFontSizes.BASE_11, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11), Color.BLACK, Color.WHITE,
            () -> ColorHelper.withOpacity(Platform.getPreferences().getAccentColor().desaturate().desaturate().darker(), 0.2),
            () -> Platform.getPreferences().getAccentColor(), 4, "atlantafx.base");
    public static final AppTheme DRACULA = new AppTheme("dracula", "dracula", new Dracula(),
            () -> AppFontSizes.forOs(AppFontSizes.BASE_11, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11), Color.web("#383f49"), Color.web("#9580ff"),
            () -> ColorHelper.withOpacity(Platform.getPreferences().getAccentColor().desaturate().desaturate().darker(), 0.2),
            () -> Platform.getPreferences().getAccentColor(), 6, "atlantafx.base");
    public static final AppTheme SPRING_DARK = new AppTheme("springDark", "springDark", new SpringDark(),
            () -> AppFontSizes.forOs(AppFontSizes.BASE_11, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11), Color.web("#0c1a10"), Color.web("#44a844"),
            () -> ColorHelper.withOpacity(Platform.getPreferences().getAccentColor().desaturate().desaturate().darker(), 0.2), () -> null, 4,
            "com.dlsc.atlantafx.themes");
    public static final AppTheme FALL_LIGHT = new AppTheme("fallLight", "fallLight", new FallLight(),
            () -> AppFontSizes.forOs(AppFontSizes.BASE_10_5, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11), Color.web("#fdf8f0"),
            Color.web("#c0a080"), () -> ColorHelper.withOpacity(Platform.getPreferences().getAccentColor().desaturate().desaturate().darker(), 0.2),
            () -> null, 4, "com.dlsc.atlantafx.themes");
    public static final AppTheme FALL_DARK = new AppTheme("fallDark", "fallDark", new FallDark(),
            () -> AppFontSizes.forOs(AppFontSizes.BASE_11, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11), Color.web("#1e0c06"), Color.web("#c88418"),
            () -> ColorHelper.withOpacity(Platform.getPreferences().getAccentColor().desaturate().desaturate().darker(), 0.2), () -> null, 4,
            "com.dlsc.atlantafx.themes");
    public static final AppTheme WINTER_DARK = new AppTheme("winterDark", "winterDark", new WinterDark(),
            () -> AppFontSizes.forOs(AppFontSizes.BASE_11, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11), Color.web("#080c18"), Color.web("#4488ff"),
            () -> ColorHelper.withOpacity(Platform.getPreferences().getAccentColor().desaturate().desaturate().darker(), 0.2), () -> null, 4,
            "com.dlsc.atlantafx.themes");
    public static final AppTheme MOCHA = new DerivedTheme("mocha", "mocha", "Mocha", new PrimerDark(),
            () -> AppFontSizes.forOs(AppFontSizes.BASE_11, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11), Color.web("#2E2E4EFF"),
            Color.web("#CDD6F4FF"), () -> ColorHelper.withOpacity(Platform.getPreferences().getAccentColor().desaturate().desaturate().darker(), 0.2),
            () -> Platform.getPreferences().getAccentColor(), 4, "atlantafx.base", 91);

    // Adjust this to create your own theme
    @SuppressWarnings("unused")
    public static final AppTheme CUSTOM = new DerivedTheme("custom", "primer", "Custom", new PrimerDark(),
            () -> AppFontSizes.forOs(AppFontSizes.BASE_10_5, AppFontSizes.BASE_10_5, AppFontSizes.BASE_11), Color.web("#0d1117"),
            Color.web("#24292f"), () -> ColorHelper.withOpacity(Platform.getPreferences().getAccentColor().desaturate().desaturate(), 0.2),
            () -> Platform.getPreferences().getAccentColor(), 4, "atlantafx.base", 91);

    // Also include your custom theme here
    public static final List<AppTheme> ALL = List.of(PRIMER_LIGHT, PRIMER_DARK, NORD_LIGHT, NORD_DARK, CUPERTINO_LIGHT, CUPERTINO_DARK, DRACULA,
            MOCHA, SPRING_DARK, FALL_LIGHT, FALL_DARK, WINTER_DARK);
    protected final String id;

    @Getter
    protected final String cssId;

    protected final atlantafx.base.theme.Theme theme;

    @Getter
    protected final Supplier<AppFontSizes> fontSizes;

    @Getter
    protected final Color baseColor;

    @Getter
    protected final Color borderColor;

    @Getter
    protected final Supplier<Color> contextMenuColor;

    @Getter
    protected final Supplier<Color> emphasisColor;

    @Getter
    protected final int displayBorderRadius;

    protected final String module;

    static AppTheme getDefaultLightTheme() {
        return switch (OsType.ofLocal()) {
            case OsType.Windows ignored -> PRIMER_LIGHT;
            case OsType.Linux ignored -> PRIMER_LIGHT;
            case OsType.MacOs ignored -> CUPERTINO_LIGHT;
        };
    }

    static AppTheme getDefaultDarkTheme() {
        return switch (OsType.ofLocal()) {
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
        var s = """
                * {
                    -color-context-menu: %s;
                }
                """.formatted(ColorHelper.toWeb(contextMenuColor.get()));
        var accentColor = emphasisColor.get();
        if (accentColor != null) {
            s += """
                 * {
                     -color-accent-fg: %s;
                     -color-accent-emphasis: %s;
                     -color-accent-muted: %s;
                     -color-accent-subtle: %s;
                 }
                 """.formatted(ColorHelper.toWeb(accentColor), ColorHelper.toWeb(accentColor.darker()), ColorHelper.toWeb(accentColor.desaturate()),
                    ColorHelper.toWeb(ColorHelper.withOpacity(accentColor.darker().desaturate().desaturate(), 0.2)));
        }
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

    public static class DerivedTheme extends AppTheme {

        private final String name;
        private final int skipLines;

        public DerivedTheme(
                String id, String cssId, String name, atlantafx.base.theme.Theme theme, Supplier<AppFontSizes> sizes, Color baseColor,
                Color borderColor, Supplier<Color> contextMenuColor, Supplier<Color> emphasisColor, int displayBorderRadius, String module,
                int skipLines
        ) {
            super(id, cssId, theme, sizes, baseColor, borderColor, contextMenuColor, emphasisColor, displayBorderRadius, module);
            this.name = name;
            this.skipLines = skipLines;
        }

        @Override
        @SneakyThrows
        public void apply() {
            var builder = new StringBuilder();
            AppResources.with(AppResources.MAIN_MODULE, "theme/" + id + ".css", path -> {
                var content = Files.readString(path);
                builder.append(content);
            });

            // Watch out for the leading slash
            AppResources.with(module, theme.getUserAgentStylesheet().substring(1), path -> {
                var baseStyleContent = Files.readString(path);
                builder.append("\n").append(baseStyleContent.lines().skip(skipLines).collect(Collectors.joining("\n")));
            });

            Application.setUserAgentStylesheet(Styles.toDataURI(builder.toString()));
        }

        @Override
        public ObservableValue<String> toTranslatedString() {
            return new SimpleStringProperty(name);
        }
    }
}
