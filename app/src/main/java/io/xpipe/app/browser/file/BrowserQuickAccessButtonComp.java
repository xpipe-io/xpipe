package io.xpipe.app.browser.file;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.platform.InputHelper;

import javafx.scene.layout.Region;

import java.util.function.Supplier;

public class BrowserQuickAccessButtonComp extends SimpleComp {

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
        button.apply(struc -> {
            struc.get().setOnAction(event -> {
                if (!cm.isShowing()) {
                    cm.showMenu(struc.get());
                } else {
                    cm.hide();
                }
                event.consume();
            });
            InputHelper.onRight(struc.get(), false, keyEvent -> {
                cm.showMenu(struc.get());
                keyEvent.consume();
            });
        });
        button.styleClass("quick-access-button");
        return button.createRegion();
    }
}
