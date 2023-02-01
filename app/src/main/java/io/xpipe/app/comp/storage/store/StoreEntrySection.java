package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.storage.StorageFilter;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.augment.GrowAugment;
import io.xpipe.extension.fxcomps.impl.HorizontalComp;
import io.xpipe.extension.fxcomps.impl.VerticalComp;
import io.xpipe.extension.fxcomps.util.BindingsHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public class StoreEntrySection implements StorageFilter.Filterable {

    public StoreEntrySection(StoreEntryWrapper entry, ObservableList<StoreEntrySection> children) {
        this.entry = entry;
        this.children = children;
    }

    public static ObservableList<StoreEntrySection> createTopLevels() {
        var topLevel = BindingsHelper.mappedContentBinding(
                StoreViewState.get()
                        .getAllEntries()
                        .filtered(storeEntryWrapper ->
                                !storeEntryWrapper.getEntry().getState().isUsable()
                                        || storeEntryWrapper
                                                        .getEntry()
                                                        .getProvider()
                                                        .getParent(storeEntryWrapper
                                                                .getEntry()
                                                                .getStore())
                                                == null),
                storeEntryWrapper -> create(storeEntryWrapper));
        var ordered = BindingsHelper.orderedContentBinding(
                topLevel,
                Comparator.<StoreEntrySection, Instant>comparing(storeEntrySection ->
                                storeEntrySection.entry.lastAccessProperty().getValue())
                        .reversed());
        return ordered;
    }

    public static StoreEntrySection create(StoreEntryWrapper e) {
        if (!e.getEntry().getState().isUsable()) {
            return new StoreEntrySection(e, FXCollections.observableArrayList());
        }

        var children = BindingsHelper.mappedContentBinding(
                StoreViewState.get()
                        .getAllEntries()
                        .filtered(other -> other.getEntry().getState().isUsable()
                                && e.getEntry()
                                        .getStore()
                                        .equals(other.getEntry()
                                                .getProvider()
                                                .getParent(other.getEntry().getStore()))),
                entry1 -> create(entry1));
        var ordered = BindingsHelper.orderedContentBinding(
                children,
                Comparator.<StoreEntrySection, Instant>comparing(storeEntrySection ->
                                storeEntrySection.entry.lastAccessProperty().getValue())
                        .reversed());
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

        if (children.size() == 0) {
            return new HorizontalComp(topEntryList);
        }

        var all = BindingsHelper.orderedContentBinding(
                children,
                Comparator.comparing(storeEntrySection ->
                        storeEntrySection.entry.lastAccessProperty().getValue()));
        var shown = BindingsHelper.filteredContentBinding(
                all,
                StoreViewState.get().getFilterString().map(s -> (storeEntrySection -> storeEntrySection.shouldShow(s))));
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
                        .apply(struc -> struc.get().setFillHeight(true))));
    }

    @Override
    public boolean shouldShow(String filter) {
        return entry.shouldShow(filter)
                || children.stream().anyMatch(storeEntrySection -> storeEntrySection.shouldShow(filter));
    }
}
