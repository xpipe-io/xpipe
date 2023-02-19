package io.xpipe.app.comp.storage.collection;

import io.xpipe.app.comp.base.CountComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.app.fxcomps.impl.FilterComp;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;

public class SourceCollectionFilterBarComp extends SimpleComp {

    private Region createGroupListHeader() {
        var label = new Label("Collections");
        label.getStyleClass().add("name");
        var count = new CountComp<>(
                SourceCollectionViewState.get().getShownGroups(),
                SourceCollectionViewState.get().getAllGroups());

        var newFolder = new IconButtonComp("mdi2f-folder-plus-outline", () -> {
                    SourceCollectionViewState.get().addNewCollection();
                })
                .shortcut(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN))
                .apply(new FancyTooltipAugment<>("addCollectionFolder"));

        var spacer = new Region();

        var topBar = new HBox(label, count.createRegion(), spacer, newFolder.createRegion());
        AppFont.header(topBar);
        topBar.setAlignment(Pos.CENTER);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getStyleClass().add("top");
        return topBar;
    }

    private Region createGroupListFilter() {
        var filter = new FilterComp(SourceCollectionViewState.get().getFilter().filterProperty());
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
        var content = new VBox(createGroupListHeader(), createGroupListFilter());
        content.getStyleClass().add("bar");
        content.getStyleClass().add("collections-bar");
        return content;
    }
}
