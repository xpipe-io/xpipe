package io.xpipe.app.comp.store;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.LeftSplitPaneComp;
import io.xpipe.app.core.AppActionLinkDetector;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.InputHelper;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.time.Duration;

public class StoreLayoutComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var stack = new StackPane();
        GlobalTimer.scheduleUntil(Duration.ofMillis(10), () -> {
            if (StoreViewState.get() == null || !StoreViewState.get().isInitialized()) {
                return false;
            }

            Platform.runLater(() -> {
                var r = createContent();
                stack.getChildren().add(r);
            });
            return true;
        });
        return stack;
    }

    private Region createContent() {
        var struc = new LeftSplitPaneComp(new StoreSidebarComp(), new StoreEntryListComp())
                .withInitialWidth(AppLayoutModel.get().getSavedState().getSidebarWidth())
                .withOnDividerChange(aDouble -> {
                    AppLayoutModel.get().getSavedState().setSidebarWidth(aDouble);
                })
                .createStructure();
        struc.getLeft().setMinWidth(270);
        struc.getLeft().setMaxWidth(500);
        struc.get().getStyleClass().add("store-layout");
        InputHelper.onKeyCombination(
                struc.get(), new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN), true, keyEvent -> {
                    AppActionLinkDetector.detectOnPaste();
                    keyEvent.consume();
                });
        return struc.get();
    }
}
