package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.IntFieldComp;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.core.OsType;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Slider;

import atlantafx.base.controls.ProgressSliderSkin;
import atlantafx.base.theme.Styles;

public class DisplayCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "display";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2m-monitor-screenshot");
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("displayOptions")
                .sub(new OptionsBuilder()
                        .pref(prefs.uiScale)
                        .addComp(
                                new IntFieldComp(prefs.uiScale).maxWidth(100).apply(struc -> {
                                    struc.get().setPromptText("100");
                                }),
                                prefs.uiScale)
                        .hide(new SimpleBooleanProperty(OsType.ofLocal() == OsType.MACOS))
                        .pref(prefs.performanceMode)
                        .addToggle(prefs.performanceMode)
                        .pref(prefs.useSystemFont)
                        .addToggle(prefs.useSystemFont)
                        .pref(prefs.censorMode)
                        .addToggle(prefs.censorMode)
                        .pref(prefs.limitedTouchscreenMode)
                        .addToggle(prefs.limitedTouchscreenMode))
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
                                        })
                                        .maxWidth(getCompWidth()),
                                prefs.windowOpacity)
                        .pref(prefs.saveWindowLocation)
                        .addToggle(prefs.saveWindowLocation)
                        .pref(prefs.focusWindowOnNotifications)
                        .addToggle(prefs.focusWindowOnNotifications))
                .buildComp();
    }
}
