package io.xpipe.app.browser;

import com.jfoenix.controls.JFXButton;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.store.ShellStore;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;

final class BookmarkList extends SimpleComp {

    private final FileBrowserModel model;

    BookmarkList(FileBrowserModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var list = DataStorage.get().getStores().stream().filter(entry -> entry.getStore() instanceof ShellStore).map(entry -> new Bookmark(entry)).toList();
        return new ListBoxViewComp<>(FXCollections.observableList(list), FXCollections.observableList(list), bookmark -> {
            var imgView =
                    new PrettyImageComp(new SimpleStringProperty(bookmark.entry().getProvider().getDisplayIconFileName()), 16, 16).createRegion();
            var button = new JFXButton(bookmark.entry().getName(), imgView);
            button.setOnAction(event -> {
                event.consume();

                var fileSystem = ((ShellStore) bookmark.entry().getStore());
                model.openFileSystem(fileSystem);
            });
            button.setAlignment(Pos.CENTER_LEFT);
            return Comp.of(() -> button).grow(true, false);
        }).createRegion();
    }
}
