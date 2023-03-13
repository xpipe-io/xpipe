package io.xpipe.app.browser;

import com.jfoenix.controls.JFXButton;
import io.xpipe.app.comp.storage.store.StoreEntryFlatMiniSection;
import io.xpipe.app.comp.storage.store.StoreEntryWrapper;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.core.store.ShellStore;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Map;

final class BookmarkList extends SimpleComp {

    private final FileBrowserModel model;

    BookmarkList(FileBrowserModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var map = StoreEntryFlatMiniSection.createMap();
        var list = new VBox();
        for (Map.Entry<StoreEntryWrapper, Region> e : map.entrySet()) {
            if (!(e.getKey().getEntry().getStore() instanceof ShellStore)) {
                continue;
            }

            var button = new JFXButton(null, e.getValue());
            button.setOnAction(event -> {
                var fileSystem = ((ShellStore) e.getKey().getEntry().getStore());
                model.openFileSystem(fileSystem);
                event.consume();
            });
            button.prefWidthProperty().bind(list.widthProperty());
            list.getChildren().add(button);
        }
        list.setFillWidth(true);
        return list;
    }
}
