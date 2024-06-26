package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.comp.store.StoreCategoryWrapper;
import io.xpipe.app.fxcomps.SimpleComp;
import javafx.scene.layout.Region;

public class StoreCategoryListComp extends SimpleComp {

    private final StoreCategoryWrapper root;

    public StoreCategoryListComp(StoreCategoryWrapper root) {
        this.root = root;
    }

    @Override
    protected Region createSimple() {
        var sp = new ScrollComp(new StoreCategoryComp(root));
        sp.styleClass("store-category-bar");
        return sp.createRegion();
    }
}
