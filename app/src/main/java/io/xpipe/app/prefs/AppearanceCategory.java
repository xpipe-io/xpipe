package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ChoiceComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.IntFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppTheme;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.OsType;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import atlantafx.base.controls.ProgressSliderSkin;
import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class AppearanceCategory extends AppPrefsCategory {

    public static OptionsBuilder themeChoice() {
        var prefs = AppPrefs.get();
        var c = ChoiceComp.ofTranslatable(prefs.theme, AppTheme.Theme.ALL, false)
                .styleClass("theme-switcher");
        c.apply(struc -> {
            Supplier<ListCell<AppTheme.Theme>> cell = () -> new ListCell<>() {
                @Override
                protected void updateItem(AppTheme.Theme theme, boolean empty) {
                    super.updateItem(theme, empty);
                    if (theme == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    setText(theme.toTranslatedString().getValue());

                    var b = new Rectangle(8, 8);
                    b.setArcWidth(theme.getDisplayBorderRadius());
                    b.setArcHeight(theme.getDisplayBorderRadius());
                    b.getStyleClass().add("dot");
                    b.setFill(theme.getBaseColor());

                    var d = new Rectangle(10, 10);
                    d.setArcWidth(theme.getDisplayBorderRadius() + 2);
                    d.setArcHeight(theme.getDisplayBorderRadius() + 2);
                    d.getStyleClass().add("dot");
                    d.setFill(theme.getBorderColor());
                    d.setFill(Color.GRAY);

                    var s = new StackPane(d, b);
                    setGraphic(s);
                    setGraphicTextGap(8);
                }
            };
            struc.get().setButtonCell(cell.get());
            struc.get().setCellFactory(themeListView -> {
                return cell.get();
            });
        });
        c.maxWidth(600 / 2);
        return new OptionsBuilder().pref(prefs.theme).addComp(c, prefs.theme);
    }

    public static OptionsBuilder languageChoice() {
        var prefs = AppPrefs.get();
        var c = ChoiceComp.ofTranslatable(prefs.language, Arrays.asList(SupportedLocale.values()), false);
        c.maxWidth(600 / 2);
        c.hgrow();
        var visit = new ButtonComp(AppI18n.observable("translate"), new FontIcon("mdi2w-web"), () -> {
            Hyperlinks.open(Hyperlinks.TRANSLATE);
        });
        var h = new HorizontalComp(List.of(c, visit)).apply(struc -> {
            struc.get().setAlignment(Pos.CENTER_LEFT);
            struc.get().setSpacing(10);
        });
        return new OptionsBuilder().pref(prefs.language).addComp(h, prefs.language);
    }

    @Override
    protected String getId() {
        return "appearance";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2b-brush");
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("uiOptions")
                .sub(new OptionsBuilder()
                        .sub(languageChoice())
                        .sub(themeChoice())
                        .pref(prefs.performanceMode)
                        .addToggle(prefs.performanceMode)
                        .pref(prefs.uiScale)
                        .addComp(
                                new IntFieldComp(prefs.uiScale).maxWidth(100).apply(struc -> {
                                    struc.get().setPromptText("100");
                                }),
                                prefs.uiScale)
                        .hide(new SimpleBooleanProperty(OsType.getLocal() == OsType.MACOS))
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
