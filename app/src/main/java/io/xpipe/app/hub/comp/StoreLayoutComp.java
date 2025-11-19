package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.DelayedInitComp;
import io.xpipe.app.comp.base.LeftSplitPaneComp;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.AppMainWindow;

import io.xpipe.app.platform.InputHelper;
import io.xpipe.app.util.ObservableSubscriber;
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
        var filterTrigger = new ObservableSubscriber();
        var left = new StoreSidebarComp(filterTrigger);
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
        comp.apply(struc -> {
            InputHelper.onKeyCombination(struc.get(), new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), false, keyEvent -> {
                filterTrigger.trigger();
                keyEvent.consume();
            });
        });
        return comp.createRegion();
    }
}
