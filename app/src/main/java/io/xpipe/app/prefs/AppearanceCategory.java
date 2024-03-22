package io.xpipe.app.prefs;

import atlantafx.base.controls.ProgressSliderSkin;
import atlantafx.base.theme.Styles;
import io.xpipe.app.core.AppTheme;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.ChoiceComp;
import io.xpipe.app.fxcomps.impl.IntFieldComp;
import io.xpipe.app.util.OptionsBuilder;
import javafx.scene.control.Slider;

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
                        .nameAndDescription("theme")
                        .addComp(ChoiceComp.ofTranslatable(prefs.theme, AppTheme.Theme.ALL, false).styleClass("theme-switcher"), prefs.theme)
                        .nameAndDescription("performanceMode")
                        .addToggle(prefs.performanceMode)
                        .nameAndDescription("uiScale")
                        .addComp(new IntFieldComp(prefs.uiScale).maxWidth(100), prefs.uiScale)
                        .nameAndDescription("useSystemFont")
                        .addToggle(prefs.useSystemFont)
                        .nameAndDescription("condenseConnectionDisplay")
                        .addToggle(prefs.condenseConnectionDisplay)
                        .nameAndDescription("showChildCategoriesInParentCategory")
                        .addToggle(prefs.showChildCategoriesInParentCategory))
                .addTitle("workflow")
                .sub(new OptionsBuilder()
                        .nameAndDescription("openConnectionSearchWindowOnConnectionCreation")
                        .addToggle(prefs.openConnectionSearchWindowOnConnectionCreation))
                .addTitle("windowOptions")
                .sub(new OptionsBuilder()
                        .nameAndDescription("windowOpacity")
                        .addComp(
                                Comp.of(() -> {
                                    var s = new Slider(0.3, 1.0, prefs.windowOpacity.get());
                                    s.getStyleClass().add(Styles.SMALL);
                                    prefs.windowOpacity.bind(s.valueProperty());
                                    s.setSkin(new ProgressSliderSkin(s));
                                    return s;
                                }),
                                prefs.windowOpacity)
                        .nameAndDescription("saveWindowLocation")
                        .addToggle(prefs.saveWindowLocation)
                        .nameAndDescription("enforceWindowModality")
                        .addToggle(prefs.enforceWindowModality))
                .buildComp();
    }
}
