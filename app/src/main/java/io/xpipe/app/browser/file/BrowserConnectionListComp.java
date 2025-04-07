package io.xpipe.app.browser.file;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.store.*;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;

import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class BrowserConnectionListComp extends SimpleComp {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private final ObservableValue<DataStoreEntry> selected;
    private final Predicate<StoreEntryWrapper> applicable;
    private final BiConsumer<StoreEntryWrapper, BooleanProperty> action;
    private final Property<StoreCategoryWrapper> category;
    private final Property<String> filter;

    public BrowserConnectionListComp(
            ObservableValue<DataStoreEntry> selected,
            Predicate<StoreEntryWrapper> applicable,
            BiConsumer<StoreEntryWrapper, BooleanProperty> action,
            Property<StoreCategoryWrapper> category,
            Property<String> filter) {
        this.selected = selected;
        this.applicable = applicable;
        this.action = action;
        this.category = category;
        this.filter = filter;
    }

    @Override
    protected Region createSimple() {
        var busyEntries = FXCollections.<StoreSection>observableSet(new HashSet<>());
        BiConsumer<StoreSection, Comp<CompStructure<Button>>> augment = (s, comp) -> {
            comp.disable(Bindings.createBooleanBinding(
                    () -> {
                        return busyEntries.contains(s) || !applicable.test(s.getWrapper());
                    },
                    busyEntries));
            comp.apply(struc -> {
                selected.addListener((observable, oldValue, newValue) -> {
                    PlatformThread.runLaterIfNeeded(() -> {
                        struc.get()
                                .pseudoClassStateChanged(
                                        SELECTED,
                                        newValue != null
                                                && newValue.equals(
                                                        s.getWrapper().getEntry()));
                    });
                });
            });
        };

        var section = new StoreSectionMiniComp(
                StoreSection.createTopLevel(
                        StoreViewState.get().getAllEntries(),
                        this::filter,
                        filter,
                        category,
                        StoreViewState.get().getEntriesListVisibilityObservable(),
                        StoreViewState.get().getEntriesListUpdateObservable()),
                augment,
                selectedAction -> {
                    BooleanProperty busy = new SimpleBooleanProperty(false);
                    action.accept(selectedAction.getWrapper(), busy);
                    busy.addListener((observable, oldValue, newValue) -> {
                        if (newValue) {
                            busyEntries.add(selectedAction);
                        } else {
                            busyEntries.remove(selectedAction);
                        }
                    });
                });

        var r = section.vgrow().createRegion();
        r.getStyleClass().add("bookmark-list");
        return r;
    }

    private boolean filter(StoreEntryWrapper w) {
        return applicable.test(w);
    }
}
