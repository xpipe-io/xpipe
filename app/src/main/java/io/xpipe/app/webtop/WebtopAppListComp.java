package io.xpipe.app.webtop;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;

public class WebtopAppListComp extends SimpleRegionBuilder {

    private final ObservableList<WebtopApp> selected;
    private final ObservableList<WebtopApp> installed = FXCollections.observableArrayList();
    private final ObservableList<WebtopApp> available = FXCollections.observableArrayList();
    private final BooleanProperty force;

    public WebtopAppListComp(ObservableList<WebtopApp> selected, BooleanProperty force) {
        this.selected = selected;
        this.force = force;
        var m = WebtopAppListManager.get();
        selected.addAll(m.getSelected());
        installed.addAll(m.getInstalled());
        available.addAll(m.getAvailable());
    }

    @Override
    protected Region createSimple() {
        var list = new ListSelectorComp<>(available, webtopApp -> AppI18n.get(webtopApp.getTranslationKey()),
                webtopApp -> {
            if (installed.contains(webtopApp)) {
                return new LabelGraphic.IconGraphic("mdi2c-check");
            } else {
                return new LabelGraphic.IconGraphic("mdi2m-minus-circle");
            }
                }, installed, webtopApp -> {
            return installed.contains(webtopApp);
        }, () -> false);

        var options = new OptionsBuilder()
                .nameAndDescription("selectApps")
                .addComp(list)
                .nameAndDescription("selectAppsForce")
                .addToggle(force);

        return options.build();
    }
}
