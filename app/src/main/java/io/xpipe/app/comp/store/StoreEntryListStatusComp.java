package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.CountComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.impl.FilterComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

public class StoreEntryListStatusComp extends SimpleComp {

    private Region createGroupListHeader() {
        var label = new Label();
        label.textProperty().bind(Bindings.createStringBinding(() -> {
            return StoreViewState.get().getActiveCategory().getValue().getRoot().equals(StoreViewState.get().getAllConnectionsCategory()) ?
                    "Connections" :
                    "Scripts";
        }, StoreViewState.get().getActiveCategory()));
        label.getStyleClass().add("name");

        var all = BindingsHelper.filteredContentBinding(StoreViewState.get().getAllEntries(), storeEntryWrapper -> {
            var storeRoot = storeEntryWrapper.getCategory().getValue().getRoot();
            return StoreViewState.get().getActiveCategory().getValue().getRoot().equals(storeRoot);
        }, StoreViewState.get().getActiveCategory());
        var shownList = BindingsHelper.filteredContentBinding(all, storeEntryWrapper -> {
            return storeEntryWrapper.shouldShow(StoreViewState.get().getFilterString().getValue());
        }, StoreViewState.get().getFilterString());
        var count = new CountComp<>(shownList, all);

        var spacer = new Region();

        var topBar = new HBox(label, spacer, count.createRegion());
        AppFont.setSize(topBar, 1);
        topBar.setAlignment(Pos.CENTER);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getStyleClass().add("top");
        return topBar;
    }

    private Region createGroupListFilter() {
        var filterProperty = new SimpleStringProperty();
        filterProperty.addListener((observable, oldValue, newValue) -> {
            ThreadHelper.runAsync(() -> {
                StoreViewState.get().getFilterString().setValue(newValue);
            });
        });
        var filter = new FilterComp(StoreViewState.get().getFilterString());
        filter.shortcut(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), s -> {
            s.getText().requestFocus();
        });
        var r = new StackPane(filter.createRegion());
        r.setAlignment(Pos.CENTER);
        r.getStyleClass().add("filter-bar");
        AppFont.medium(r);
        return r;
    }

    private Region createButtons() {
        var menu = new MenuButton(AppI18n.get("addConnections"), new FontIcon("mdi2p-plus-thick"));
        AppFont.medium(menu);
        GrowAugment.create(true, false).augment(menu);
        StoreCreationMenu.addButtons(menu);
        menu.setOpacity(0.85);
        return menu;
    }

    @Override
    public Region createSimple() {
        var bar = new VBox(createGroupListHeader(), createGroupListFilter(), createButtons());
        bar.setFillWidth(true);
        bar.getStyleClass().add("bar");
        bar.getStyleClass().add("store-header-bar");
        return bar;
    }
}
