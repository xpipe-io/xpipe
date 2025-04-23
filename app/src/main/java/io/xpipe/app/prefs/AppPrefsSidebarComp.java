package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppRestart;
import io.xpipe.app.util.PlatformThread;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class AppPrefsSidebarComp extends SimpleComp {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    @Override
    protected Region createSimple() {
        var effectiveCategories = AppPrefs.get().getCategories().stream()
                .filter(appPrefsCategory -> appPrefsCategory.show())
                .toList();
        var buttons = effectiveCategories.stream()
                .<Comp<?>>map(appPrefsCategory -> {
                    return new ButtonComp(AppI18n.observable(appPrefsCategory.getId()), () -> {
                                AppPrefs.get().getSelectedCategory().setValue(appPrefsCategory);
                            })
                            .apply(struc -> {
                                struc.get().setTextAlignment(TextAlignment.LEFT);
                                struc.get().setAlignment(Pos.CENTER_LEFT);
                                AppPrefs.get().getSelectedCategory().subscribe(val -> {
                                    struc.get().pseudoClassStateChanged(SELECTED, appPrefsCategory.equals(val));
                                });
                            })
                            .grow(true, false);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        var restartButton = new ButtonComp(AppI18n.observable("restartApp"), new FontIcon("mdi2r-restart"), () -> {
            AppRestart.restart();
        });
        restartButton.grow(true, false);
        restartButton.visible(AppPrefs.get().getRequiresRestart());
        restartButton.padding(new Insets(6, 10, 6, 6));
        buttons.add(Comp.vspacer());
        buttons.add(restartButton);

        var vbox = new VerticalComp(buttons).styleClass("sidebar");
        vbox.apply(struc -> {
            AppPrefs.get().getSelectedCategory().subscribe(val -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    var index = val != null ? effectiveCategories.indexOf(val) : 0;
                    if (index >= struc.get().getChildren().size()) {
                        return;
                    }

                    ((Button) struc.get().getChildren().get(index)).fire();
                });
            });
        });
        return vbox.createRegion();
    }
}
