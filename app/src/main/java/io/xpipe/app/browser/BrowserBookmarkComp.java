package io.xpipe.app.browser;

import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.comp.store.StoreSectionMiniComp;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FilterComp;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DataStoreCategoryChoiceComp;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import atlantafx.base.theme.Styles;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class BrowserBookmarkComp extends SimpleComp {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private final ObservableValue<DataStoreEntry> selected;
    private final Predicate<StoreEntryWrapper> applicable;
    private final BiConsumer<StoreEntryWrapper, BooleanProperty> action;

    public BrowserBookmarkComp(
            ObservableValue<DataStoreEntry> selected,
            Predicate<StoreEntryWrapper> applicable,
            BiConsumer<StoreEntryWrapper, BooleanProperty> action) {
        this.selected = selected;
        this.applicable = applicable;
        this.action = action;
    }

    @Override
    protected Region createSimple() {
        var filterText = new SimpleStringProperty();
        var selectedCategory = new SimpleObjectProperty<>(
                StoreViewState.get().getActiveCategory().getValue());

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
                        StoreViewState.get().getAllEntries(), storeEntryWrapper -> true, filterText, selectedCategory),
                augment,
                entryWrapper -> action.accept(entryWrapper, busy),
                true);
        var category = new DataStoreCategoryChoiceComp(
                        StoreViewState.get().getAllConnectionsCategory(),
                        StoreViewState.get().getActiveCategory(),
                        selectedCategory)
                .styleClass(Styles.LEFT_PILL)
                .minWidth(Region.USE_PREF_SIZE);
        var filter = new FilterComp(filterText).hgrow();

        var top = new HorizontalComp(List.of(category, filter))
                .styleClass("categories")
                .apply(struc -> {
                    AppFont.medium(struc.get());
                    struc.get().setFillHeight(true);
                })
                .createRegion();
        var r = section.vgrow().createRegion();
        var content = new VBox(top, r);
        content.setFillWidth(true);

        content.getStyleClass().add("bookmark-list");
        return content;
    }
}
