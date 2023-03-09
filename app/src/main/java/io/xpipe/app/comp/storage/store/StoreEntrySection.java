package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.storage.StorageFilter;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public class StoreEntrySection implements StorageFilter.Filterable {

    private static final Comparator<StoreEntrySection> COMPARATOR = Comparator.<StoreEntrySection, Instant>comparing(
                    o -> o.entry.getEntry().getState().equals(DataStoreEntry.State.COMPLETE_AND_VALID)
                            ? o.entry.getEntry().getLastAccess()
                            : Instant.EPOCH).reversed()
            .thenComparing(
                    storeEntrySection -> storeEntrySection.entry.getEntry().getName());

    public StoreEntrySection(StoreEntryWrapper entry, ObservableList<StoreEntrySection> children) {
        this.entry = entry;
        this.children = children;
    }

    public static ObservableList<StoreEntrySection> createTopLevels() {
        var filtered =
                BindingsHelper.filteredContentBinding(StoreViewState.get().getAllEntries(), storeEntryWrapper -> {
                    if (!storeEntryWrapper.getEntry().getState().isUsable()) {
                        return true;
                    }

                    var parent = storeEntryWrapper
                            .getEntry()
                            .getProvider()
                            .getParent(storeEntryWrapper.getEntry().getStore());
                    return parent == null
                            || (DataStorage.get().getStoreEntryIfPresent(parent).isEmpty());
                });
        var topLevel = BindingsHelper.mappedContentBinding(filtered, storeEntryWrapper -> create(storeEntryWrapper));
        var ordered = BindingsHelper.orderedContentBinding(
                topLevel,
                COMPARATOR);
        return ordered;
    }

    public static StoreEntrySection create(StoreEntryWrapper e) {
        if (!e.getEntry().getState().isUsable()) {
            return new StoreEntrySection(e, FXCollections.observableArrayList());
        }

        var filtered = BindingsHelper.filteredContentBinding(
                StoreViewState.get().getAllEntries(),
                other -> other.getEntry().getState().isUsable()
                        && e.getEntry()
                                .getStore()
                                .equals(other.getEntry()
                                        .getProvider()
                                        .getParent(other.getEntry().getStore())));
        var children = BindingsHelper.mappedContentBinding(filtered, entry1 -> create(entry1));
        var ordered = BindingsHelper.orderedContentBinding(
                children,
                COMPARATOR);
        return new StoreEntrySection(e, ordered);
    }

    private final StoreEntryWrapper entry;
    private final ObservableList<StoreEntrySection> children;

    public Comp<?> comp(boolean top) {
        var root = new StoreEntryComp(entry).apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS));
        var icon = Comp.of(() -> {
            var padding = new FontIcon("mdal-arrow_forward_ios");
            padding.setIconSize(14);
            var pain = new StackPane(padding);
            pain.setMinWidth(20);
            pain.setMaxHeight(20);
            return pain;
        });
        List<Comp<?>> topEntryList = top ? List.of(root) : List.of(icon, root);

        var all = children;
        var shown = BindingsHelper.filteredContentBinding(
                all,
                StoreViewState.get()
                        .getFilterString()
                        .map(s -> (storeEntrySection -> storeEntrySection.shouldShow(s))));
        var content = new ListBoxViewComp<>(shown, all, (StoreEntrySection e) -> {
                    return e.comp(false).apply(GrowAugment.create(true, false));
                })
                .apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS))
                .apply(struc -> struc.get().backgroundProperty().set(Background.fill(Color.color(0, 0, 0, 0.01))));
        var spacer = Comp.of(() -> {
            var padding = new Region();
            padding.setMinWidth(25);
            padding.setMaxWidth(25);
            return padding;
        });
        return new VerticalComp(List.of(
                new HorizontalComp(topEntryList),
                new HorizontalComp(List.of(spacer, content))
                        .apply(struc -> struc.get().setFillHeight(true))
                        .hide(BindingsHelper.persist(Bindings.size(children).isEqualTo(0)))));
    }

    @Override
    public boolean shouldShow(String filter) {
        return entry.shouldShow(filter)
                || children.stream().anyMatch(storeEntrySection -> storeEntrySection.shouldShow(filter));
    }
}
