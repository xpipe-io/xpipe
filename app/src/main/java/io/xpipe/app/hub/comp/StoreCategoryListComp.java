package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ScrollComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.core.process.OsType;

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
            struc.get().setFitToWidth(true);
            if (OsType.getLocal() == OsType.MACOS) {
                AppFontSizes.lg(struc.get());
            }
            struc.get()
                    .minHeightProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                var h = content.getHeight();
                                return Math.min(150, h + 2);
                            },
                            content.heightProperty()));
        });
        return sp.createRegion();
    }
}
