package io.xpipe.app.browser;

import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.layout.Region;

import java.util.function.Supplier;

public class BrowserQuickAccessButtonComp extends SimpleComp {

    private final Supplier<BrowserEntry> base;
    private final OpenFileSystemModel model;

    public BrowserQuickAccessButtonComp(Supplier<BrowserEntry> base, OpenFileSystemModel model) {
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
            cm.addEventFilter(Menu.ON_HIDDEN, e -> {
                Platform.runLater(() -> {
                    struc.get().requestFocus();
                });
            });
            BrowserQuickAccessContextMenu.onRight(struc.get(), false, keyEvent -> {
                cm.showMenu(struc.get());
                keyEvent.consume();
            });
        });
        button.styleClass("quick-access-button");
        return button.createRegion();
    }
}
