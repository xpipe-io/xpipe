package io.xpipe.app.comp.store;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ScrollComp;

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
