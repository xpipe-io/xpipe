package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.DelayedInitComp;
import io.xpipe.app.comp.base.LeftSplitPaneComp;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.platform.InputHelper;
import io.xpipe.app.terminal.TerminalDockHubComp;
import io.xpipe.app.terminal.TerminalDockHubManager;
import io.xpipe.app.util.ObservableSubscriber;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class StoreLayoutComp extends SimpleRegionBuilder {

    @Override
    protected Region createSimple() {
        var delayed = new DelayedInitComp(
                RegionBuilder.of(() -> createContent()),
                () -> StoreViewState.get() != null && StoreViewState.get().isInitialized());
        return delayed.build();
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
        comp.apply(struc -> {
            InputHelper.onKeyCombination(
                    struc, new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), false, keyEvent -> {
                        filterTrigger.trigger();
                        keyEvent.consume();
                    });
        });

        var stack = new StackPane(comp.build());
        stack.getStyleClass().add("store-layout");

        if (TerminalDockHubManager.isAvailable()) {
            var model = TerminalDockHubManager.get();
            var dock = new TerminalDockHubComp(model.getDockModel());
            stack.getChildren().add(dock.build());
        }

        return stack;
    }
}
