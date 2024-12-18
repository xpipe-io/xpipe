package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.VerticalComp;

import javafx.scene.layout.Region;

import java.util.List;

public class StoreSidebarComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var sideBar = new VerticalComp(List.of(
                new StoreEntryListOverviewComp()
                        .styleClass("color-box")
                        .styleClass("gray")
                        .styleClass("bar"),
                new StoreCategoryListComp(StoreViewState.get().getAllConnectionsCategory())
                        .styleClass("color-box")
                        .styleClass("gray")
                        .styleClass("bar"),
                new StoreCategoryListComp(StoreViewState.get().getAllScriptsCategory())
                        .styleClass("color-box")
                        .styleClass("gray")
                        .styleClass("bar"),
                new StoreCategoryListComp(StoreViewState.get().getAllIdentitiesCategory())
                        .styleClass("color-box")
                        .styleClass("gray")
                        .styleClass("bar"),
                Comp.of(() -> new Region())
                        .styleClass("color-box")
                        .styleClass("gray")
                        .styleClass("bar")
                        .styleClass("filler-bar")
                        .minHeight(10)
                        .vgrow()));
        sideBar.apply(struc -> struc.get().setFillWidth(true));
        sideBar.styleClass("sidebar");
        sideBar.prefWidth(240);
        return sideBar.createRegion();
    }
}
