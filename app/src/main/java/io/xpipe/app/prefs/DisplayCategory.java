package io.xpipe.app.prefs;

import atlantafx.base.controls.ProgressSliderSkin;
import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ChoiceComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.IntFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppTheme;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.core.OsType;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

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
                        .pref(prefs.limitedTouchscreenMode)
                        .addToggle(prefs.limitedTouchscreenMode)
                        .pref(prefs.useSystemFont)
                        .addToggle(prefs.useSystemFont)
                        .pref(prefs.censorMode)
                        .addToggle(prefs.censorMode))
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
