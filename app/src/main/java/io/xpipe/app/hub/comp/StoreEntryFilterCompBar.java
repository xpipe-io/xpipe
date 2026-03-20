package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.util.ObservableSubscriber;

import javafx.scene.layout.Region;

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
