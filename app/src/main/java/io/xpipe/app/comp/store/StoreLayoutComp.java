package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.DelayedInitComp;
import io.xpipe.app.comp.base.LeftSplitPaneComp;
import io.xpipe.app.core.AppActionLinkDetector;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.util.InputHelper;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
        left.minWidth(270);
        left.maxWidth(500);
        var comp = new LeftSplitPaneComp(left, new StoreEntryListComp())
                .withInitialWidth(AppLayoutModel.get().getSavedState().getSidebarWidth())
                .withOnDividerChange(aDouble -> {
                    AppLayoutModel.get().getSavedState().setSidebarWidth(aDouble);
                });
        comp.styleClass("store-layout");
        comp.apply(struc -> {
            InputHelper.onKeyCombination(
                    struc.get(), new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN), true, keyEvent -> {
                        AppActionLinkDetector.detectOnPaste();
                        keyEvent.consume();
                    });
        });
        return comp.createRegion();
    }
}
