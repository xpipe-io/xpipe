package io.xpipe.app.browser.file;

import io.xpipe.app.comp.RegionDescriptor;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.platform.InputHelper;

import javafx.scene.layout.Region;

import java.util.function.Supplier;

public class BrowserQuickAccessButtonComp extends SimpleRegionBuilder {

    private final Supplier<BrowserEntry> base;
    private final BrowserFileSystemTabModel model;

    public BrowserQuickAccessButtonComp(Supplier<BrowserEntry> base, BrowserFileSystemTabModel model) {
        this.base = base;
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var cm = new BrowserQuickAccessContextMenu(base, model);
        var button = new IconButtonComp("mdi2c-chevron-double-right");
        button.describe(d -> d.nameKey("quickAccess").focusTraversal(RegionDescriptor.FocusTraversal.DISABLED));
        button.apply(struc -> {
            struc.setOnAction(event -> {
                if (!cm.isShowing()) {
                    cm.showMenu(struc);
                } else {
                    cm.hide();
                }
                event.consume();
            });
            InputHelper.onRight(struc, false, keyEvent -> {
                cm.showMenu(struc);
                keyEvent.consume();
            });
        });
        button.style("quick-access-button");
        return button.build();
    }
}
