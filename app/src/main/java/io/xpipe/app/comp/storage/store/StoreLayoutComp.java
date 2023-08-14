package io.xpipe.app.comp.storage.store;

import io.xpipe.app.core.AppActionLinkDetector;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

public class StoreLayoutComp extends SimpleComp {

    public StoreLayoutComp() {
        shortcut(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN), structure -> {
            AppActionLinkDetector.detectOnPaste();
        });
    }

    @Override
    protected Region createSimple() {
        var listComp = new StoreEntryListComp().apply(GrowAugment.create(false, true));
        var r = new BorderPane();

        var listR = listComp.createRegion();
        var groupHeader = new StoreSidebarComp().createRegion();
        r.setLeft(groupHeader);
        r.setCenter(listR);
        r.getStyleClass().add("layout");
        return r;
    }
}
