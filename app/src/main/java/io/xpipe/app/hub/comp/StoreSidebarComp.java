package io.xpipe.app.hub.comp;



import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.util.ObservableSubscriber;

import javafx.scene.layout.Region;

import java.util.List;

public class StoreSidebarComp extends SimpleRegionBuilder {

    private final ObservableSubscriber filterTrigger;

    public StoreSidebarComp(ObservableSubscriber filterTrigger) {
        this.filterTrigger = filterTrigger;
    }

    @Override
    protected Region createSimple() {
        var sideBar = new VerticalComp(List.of(
                new StoreEntryListOverviewComp(filterTrigger)
                        .style("color-box")
                        .style("gray")
                        .style("bar"),
                new StoreCategoryListComp(StoreViewState.get().getAllConnectionsCategory())
                        .style("color-box")
                        .style("gray")
                        .style("bar"),
                new StoreCategoryListComp(StoreViewState.get().getAllScriptsCategory())
                        .style("color-box")
                        .style("gray")
                        .style("bar"),
                new StoreCategoryListComp(StoreViewState.get().getAllIdentitiesCategory())
                        .style("color-box")
                        .style("gray")
                        .style("bar"),
                RegionBuilder.of(() -> new Region())
                        .style("color-box")
                        .style("gray")
                        .style("bar")
                        .style("filler-bar")
                        .minHeight(10)
                        .vgrow()));
        sideBar.apply(struc -> struc.setFillWidth(true));
        sideBar.style("sidebar");
        sideBar.prefWidth(240);
        return sideBar.build();
    }
}
