package io.xpipe.app.browser;

import io.xpipe.app.comp.store.*;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStoreEntry;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class BrowserBookmarkComp extends SimpleComp {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private final ObservableValue<DataStoreEntry> selected;
    private final Predicate<StoreEntryWrapper> applicable;
    private final BiConsumer<StoreEntryWrapper, BooleanProperty> action;
    private final Property<StoreCategoryWrapper> category;
    private final Property<String> filter ;

    public BrowserBookmarkComp(
            ObservableValue<DataStoreEntry> selected,
            Predicate<StoreEntryWrapper> applicable,
            BiConsumer<StoreEntryWrapper, BooleanProperty> action, Property<StoreCategoryWrapper> category, Property<String> filter
    ) {
        this.selected = selected;
        this.applicable = applicable;
        this.action = action;
        this.category = category;
        this.filter = filter;
    }

    @Override
    protected Region createSimple() {
        BooleanProperty busy = new SimpleBooleanProperty(false);
        BiConsumer<StoreSection, Comp<CompStructure<Button>>> augment = (s, comp) -> {
            comp.disable(Bindings.createBooleanBinding(
                    () -> {
                        return busy.get() || !applicable.test(s.getWrapper());
                    },
                    busy));
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
                        StoreViewState.get().getAllEntries(), this::filter, filter, category, StoreViewState.get().getEntriesListUpdateObservable()),
                augment,
                entryWrapper -> action.accept(entryWrapper, busy),
                true);

        var r = section.vgrow().createRegion();
        r.getStyleClass().add("bookmark-list");
        return r;
    }

    private boolean filter(StoreEntryWrapper w) {
        return applicable.test(w);
    }
}
