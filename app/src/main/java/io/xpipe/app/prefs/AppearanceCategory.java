package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppTheme;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ChoiceComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.IntFieldComp;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.OptionsBuilder;

import javafx.geometry.Pos;
import javafx.scene.control.Slider;

import atlantafx.base.controls.ProgressSliderSkin;
import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class AppearanceCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "appearance";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("uiOptions")
                .sub(new OptionsBuilder()
                        .pref(prefs.language)
                        .addComp(languageChoice(), prefs.language)
                        .pref(prefs.theme)
                        .addComp(
                                ChoiceComp.ofTranslatable(prefs.theme, AppTheme.Theme.ALL, false)
                                        .styleClass("theme-switcher"),
                                prefs.theme)
                        .pref(prefs.performanceMode)
                        .addToggle(prefs.performanceMode)
                        .pref(prefs.uiScale)
                        .addComp(new IntFieldComp(prefs.uiScale).maxWidth(100), prefs.uiScale)
                        .pref(prefs.useSystemFont)
                        .addToggle(prefs.useSystemFont))
                .addTitle("windowOptions")
                .sub(new OptionsBuilder()
                        .pref(prefs.windowOpacity)
                        .addComp(
                                Comp.of(() -> {
                                    var s = new Slider(0.3, 1.0, prefs.windowOpacity.get());
                                    s.getStyleClass().add(Styles.SMALL);
                                    prefs.windowOpacity.bind(s.valueProperty());
                                    s.setSkin(new ProgressSliderSkin(s));
                                    return s;
                                }),
                                prefs.windowOpacity)
                        .pref(prefs.saveWindowLocation)
                        .addToggle(prefs.saveWindowLocation)
                        .pref(prefs.enforceWindowModality)
                        .addToggle(prefs.enforceWindowModality))
                .buildComp();
    }

    private Comp<?> languageChoice() {
        var prefs = AppPrefs.get();
        var c = ChoiceComp.ofTranslatable(prefs.language, SupportedLocale.ALL, false);
        var visit = new ButtonComp(AppI18n.observable("translate"), new FontIcon("mdi2w-web"), () -> {
            Hyperlinks.open(Hyperlinks.TRANSLATE);
        });
        return new HorizontalComp(List.of(c, visit)).apply(struc -> {
            struc.get().setAlignment(Pos.CENTER_LEFT);
            struc.get().setSpacing(10);
        });
    }
}
