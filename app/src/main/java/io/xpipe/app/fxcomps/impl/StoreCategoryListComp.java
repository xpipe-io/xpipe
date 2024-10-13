package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.comp.store.StoreCategoryWrapper;
import io.xpipe.app.fxcomps.SimpleComp;

import javafx.beans.binding.Bindings;
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
        sp.apply(struc -> {
            Region content = (Region) struc.get().getContent();
            struc.get()
                    .minHeightProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                var h = content.getHeight();
                                return Math.min(200, h + 2);
                            },
                            content.heightProperty()));
        });
        return sp.createRegion();
    }
}
