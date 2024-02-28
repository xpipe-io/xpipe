package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.comp.store.StoreCategoryWrapper;
import io.xpipe.app.fxcomps.SimpleComp;
import javafx.scene.layout.Region;

import java.util.List;

public class StoreCategoryListComp extends SimpleComp {

    private final StoreCategoryWrapper root;

    public StoreCategoryListComp(StoreCategoryWrapper root) {
        this.root = root;
    }

    @Override
    protected Region createSimple() {
        return new VerticalComp(List.of(new StoreCategoryComp(root)))
                .apply(struc -> struc.get().setFillWidth(true))
                .apply(struc -> struc.get().setSpacing(3))
                .styleClass("store-category-bar")
                .createRegion();
    }
}
