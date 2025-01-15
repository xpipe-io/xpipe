package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.util.ContextMenuHelper;
import io.xpipe.app.util.LabelGraphic;

import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.function.Consumer;

public class StoreQuickAccessButtonComp extends Comp<CompStructure<Button>> {

    private final StoreSection section;
    private final Consumer<StoreSection> action;

    public StoreQuickAccessButtonComp(StoreSection section, Consumer<StoreSection> action) {
        this.section = section;
        this.action = action;
    }

    private ContextMenu createMenu() {
        if (section.getShownChildren().getList().isEmpty()) {
            return null;
        }

        var cm = ContextMenuHelper.create();
        cm.getStyleClass().add("condensed");
        Menu menu = (Menu) recurse(cm, section);
        cm.getItems().addAll(menu.getItems());
        return cm;
    }

    private MenuItem recurse(ContextMenu contextMenu, StoreSection section) {
        var c = section.getShownChildren();
        var w = section.getWrapper();
        var graphic = w.getEntry().getEffectiveIconFile();
        if (c.getList().isEmpty()) {
            var item = new MenuItem(w.getName().getValue(),  new LabelGraphic.ImageGraphic(graphic, 16).createGraphicNode());
            item.setOnAction(event -> {
                action.accept(section);
                contextMenu.hide();
                event.consume();
            });
            return item;
        }

        var items = new ArrayList<MenuItem>();
        for (StoreSection sub : c.getList()) {
            if (!sub.getWrapper().getValidity().getValue().isUsable()) {
                continue;
            }

            items.add(recurse(contextMenu, sub));
        }
        var m = new Menu(
                w.getName().getValue(),
                PrettyImageHelper.ofFixedSizeSquare(graphic, 16).createRegion());
        m.getItems().setAll(items);
        m.setOnAction(event -> {
            if (event.getTarget() == m) {
                if (m.getItems().isEmpty()) {
                    return;
                }

                action.accept(section);
                contextMenu.hide();
                event.consume();
            }
        });
        return m;
    }

    @Override
    public CompStructure<Button> createBase() {
        var button = new IconButtonComp("mdi2c-chevron-double-right");
        button.apply(struc -> {
            struc.get().setOnAction(event -> {
                var cm = createMenu();
                if (cm == null) {
                    return;
                }

                ContextMenuHelper.toggleShow(cm, struc.get(), Side.RIGHT);
                event.consume();
            });
        });
        return button.createStructure();
    }
}
