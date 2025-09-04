package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.DelayedInitComp;
import io.xpipe.app.comp.base.LeftSplitPaneComp;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.AppMainWindow;

import javafx.scene.layout.Region;

public class StoreLayoutComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var delayed = new DelayedInitComp(
                Comp.of(() -> createContent()),
                () -> StoreViewState.get() != null && StoreViewState.get().isInitialized());
        return delayed.createRegion();
    }

    private Region createContent() {
        var left = new StoreSidebarComp();
        left.hide(AppMainWindow.get().getStage().widthProperty().lessThan(1000));
        left.minWidth(270);
        left.maxWidth(500);
        left.minHeight(0);
        var comp = new LeftSplitPaneComp(left, new StoreEntryListComp())
                .withInitialWidth(AppLayoutModel.get().getSavedState().getSidebarWidth())
                .withOnDividerChange(aDouble -> {
                    if (aDouble == 0.0) {
                        return;
                    }

                    AppLayoutModel.get().getSavedState().setSidebarWidth(aDouble);
                });
        comp.styleClass("store-layout");
        return comp.createRegion();
    }
}
