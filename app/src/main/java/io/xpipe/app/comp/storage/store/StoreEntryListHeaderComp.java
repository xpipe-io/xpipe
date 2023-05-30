package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.base.CountComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FilterComp;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;

public class StoreEntryListHeaderComp extends SimpleComp {

    private Region createGroupListHeader() {
        var label = new Label("Connections");
        label.getStyleClass().add("name");
        var count = new CountComp<>(
                StoreViewState.get().getShownEntries(), StoreViewState.get().getAllEntries());

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
                StoreViewState.get().getFilter().filterProperty().setValue(newValue);
            });
        });
        var filter = new FilterComp(StoreViewState.get().getFilter().filterProperty());
        filter.shortcut(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), s -> {
            s.getText().requestFocus();
        });
        var r = new StackPane(filter.createRegion());
        r.setAlignment(Pos.CENTER);
        r.getStyleClass().add("filter-bar");
        AppFont.medium(r);
        return r;
    }

    @Override
    public Region createSimple() {
        var bar = new VBox(createGroupListHeader(), createGroupListFilter());
        bar.getStyleClass().add("bar");
        bar.getStyleClass().add("store-header-bar");
        return bar;
    }
}
