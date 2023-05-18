package io.xpipe.app.comp.storage.store;

import javafx.collections.ListChangeListener;
import javafx.scene.control.TreeItem;

public class StoreEntryTree {

    public static TreeItem<StoreEntryWrapper> createTree() {
        var topLevel = StoreSection.createTopLevel();
        var root = new TreeItem<StoreEntryWrapper>();
        root.setExpanded(true);

        // Listen for any entry list change, not only top level changes
        StoreViewState.get().getAllEntries().addListener((ListChangeListener<? super StoreEntryWrapper>) c -> {
            root.getChildren().clear();
            for (StoreSection v : topLevel.getChildren()) {
                add(root, v);
            }
        });

        for (StoreSection v : topLevel.getChildren()) {
            add(root, v);
        }

        return root;
    }

    private static void add(TreeItem<StoreEntryWrapper> parent, StoreSection section) {
        var item = new TreeItem<>(section.getWrapper());
        item.setExpanded(section.getWrapper().getExpanded().getValue());
        parent.getChildren().add(item);
        for (StoreSection child : section.getChildren()) {
            add(item, child);
        }
    }
}
