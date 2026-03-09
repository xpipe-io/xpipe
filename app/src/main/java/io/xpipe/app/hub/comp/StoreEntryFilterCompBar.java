package io.xpipe.app.hub.comp;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.util.ObservableSubscriber;
import javafx.scene.layout.Region;

import java.util.List;

public class StoreEntryFilterCompBar extends SimpleRegionBuilder {

    private final ObservableSubscriber filterTrigger;

    public StoreEntryFilterCompBar(ObservableSubscriber filterTrigger) {
        this.filterTrigger = filterTrigger;
    }

    @Override
    public Region createSimple() {
        var bar = new StoreFilterFieldComp(filterTrigger);
        bar.style("bar");
        bar.style("store-filter-bar");
        return bar.build();
    }
}
