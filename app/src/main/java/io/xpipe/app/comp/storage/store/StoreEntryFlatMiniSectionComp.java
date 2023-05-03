package io.xpipe.app.comp.storage.store;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.storage.DataStoreEntry;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class StoreEntryFlatMiniSectionComp extends SimpleComp {

    public static final ObservableList<StoreEntryFlatMiniSectionComp> ALL = FXCollections.observableArrayList();

    static {
        var topLevel = StoreSection.createTopLevel();

        // Listen for any entry list change, not only top level changes
        StoreViewState.get().getAllEntries().addListener((ListChangeListener<? super StoreEntryWrapper>) c -> {
            ALL.clear();
            var depth = 0;
            for (StoreSection v : topLevel.getChildren()) {
                System.out.println(v.getWrapper().getEntry().getName() + " " + v.getChildren().size());
                add(depth, v);
            }
        });

        var depth = 0;
        for (StoreSection v : topLevel.getChildren()) {
            add(depth, v);
        }
    }

    private static void add(int depth, StoreSection section) {
        ALL.add(new StoreEntryFlatMiniSectionComp(depth, section.getWrapper().getEntry()));
        for (StoreSection child : section.getChildren()) {
            add(depth + 1, child);
        }
    }

    int depth;
    DataStoreEntry entry;

    @Override
    protected Region createSimple() {
        var image = entry.getState() == DataStoreEntry.State.LOAD_FAILED
                ? "disabled_icon.png"
                : entry.getProvider().getDisplayIconFileName(entry.getStore());
        var label = new Label(entry.getName(), new PrettyImageComp(new SimpleStringProperty(image), 20, 20).createRegion());
        var spacer = new Spacer(depth * 10, Orientation.HORIZONTAL);
        var box = new HBox(spacer, label);
        return box;
    }
}
