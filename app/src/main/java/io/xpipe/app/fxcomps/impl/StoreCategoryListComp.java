package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.comp.storage.store.StoreViewState;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import javafx.scene.layout.Region;

import java.util.List;

public class StoreCategoryListComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var all = StoreViewState.get().getAllConnectionsCategory();
        var scripts = StoreViewState.get().getAllScriptsCategory();
        return new VerticalComp(List.of(
                        new StoreCategoryComp(all),
                        Comp.vspacer(10),
                        new StoreCategoryComp(scripts)))
                .apply(struc -> struc.get().setFillWidth(true))
                .apply(struc -> struc.get().setSpacing(3))
                .styleClass("store-category-bar")
                .createRegion();
    }
}
