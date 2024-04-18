package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.PlatformThread;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;

public class AppPrefsSidebarComp extends SimpleComp {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    @Override
    protected Region createSimple() {
        var buttons = AppPrefs.get().getCategories().stream()
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
                .toList();
        var vbox = new VerticalComp(buttons).styleClass("sidebar");
        vbox.apply(struc -> {
            AppPrefs.get().getSelectedCategory().subscribe(val -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    var index = val != null ? AppPrefs.get().getCategories().indexOf(val) : 0;
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
