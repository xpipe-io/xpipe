package io.xpipe.app.prefs;



import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppRestart;
import io.xpipe.app.platform.PlatformThread;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;

import org.int4.fx.builders.common.AbstractRegionBuilder;
import io.xpipe.app.comp.BaseRegionBuilder;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class AppPrefsSidebarComp extends SimpleRegionBuilder {

    @Override
    protected Region createSimple() {
        var effectiveCategories = AppPrefs.get().getCategories().stream()
                .filter(appPrefsCategory -> appPrefsCategory.show())
                .toList();
        var buttons = effectiveCategories.stream()
                .<BaseRegionBuilder<?,?>>map(appPrefsCategory -> {
                    return new ButtonComp(
                                    AppI18n.observable(appPrefsCategory.getId()),
                                    new ReadOnlyObjectWrapper<>(appPrefsCategory.getIcon()),
                                    () -> {
                                        AppPrefs.get().getSelectedCategory().setValue(appPrefsCategory);
                                    })
                            .apply(struc -> {
                                struc.setGraphicTextGap(9);
                                struc.setTextAlignment(TextAlignment.LEFT);
                                struc.setAlignment(Pos.CENTER_LEFT);
                                AppPrefs.get().getSelectedCategory().subscribe(val -> {
                                    struc
                                            .pseudoClassStateChanged(
                                                    PseudoClass.getPseudoClass("selected"),
                                                    appPrefsCategory.equals(val));
                                });
                            })
                            .maxWidth(2000);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        var restartButton = new ButtonComp(AppI18n.observable("restartApp"), new FontIcon("mdi2r-restart"), () -> {
            AppRestart.restart();
        });
        restartButton.maxWidth(2000);
        restartButton.visible(AppPrefs.get().getRequiresRestart());
        restartButton.padding(new Insets(6, 10, 6, 6));
        buttons.add(RegionBuilder.vspacer());
        buttons.add(restartButton);

        var vbox = new VerticalComp(buttons)
                .style("sidebar")
                .style("color-box")
                .style("gray");
        vbox.apply(struc -> {
            AppPrefs.get().getSelectedCategory().subscribe(val -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    var index = val != null ? effectiveCategories.indexOf(val) : 0;
                    if (index >= struc.getChildren().size()) {
                        return;
                    }

                    ((Button) struc.getChildren().get(index)).fire();
                });
            });
        });
        return vbox.build();
    }
}
