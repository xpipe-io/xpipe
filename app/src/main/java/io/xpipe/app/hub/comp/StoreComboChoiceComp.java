package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.layout.Region;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.function.Function;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class StoreComboChoiceComp<T extends DataStore> extends SimpleComp {

    @Value
    public static class ComboValue<T extends DataStore> {

        public static <T extends DataStore> ComboValue<T> of(String manualHost, DataStoreEntryRef<T> ref) {
            var manualNull = manualHost == null || manualHost.isEmpty();
            if (manualNull && ref == null) {
                return null;
            }

            return manualNull ? new ComboValue<>(null, ref) : new ComboValue<>(manualHost, null);
        }

        String manualHost;
        DataStoreEntryRef<T> ref;
    }

    private final Property<ComboValue<T>> selected;
    private final Function<T, String> stringConverter;
    private final StoreChoicePopover<T> popover;

    public StoreComboChoiceComp(
            Function<T, String> stringConverter,
            DataStoreEntry self,
            Property<ComboValue<T>> selected,
            Class<?> storeClass,
            Predicate<DataStoreEntryRef<T>> applicableCheck,
            StoreCategoryWrapper initialCategory) {
        this.stringConverter = stringConverter;
        this.selected = selected;

        var popoverProp = new SimpleObjectProperty<>(
                selected.getValue() != null ? selected.getValue().getRef() : null);
        popoverProp.subscribe(tDataStoreEntryRef -> {
            if (tDataStoreEntryRef != null) {
                selected.setValue(new ComboValue<>(null, tDataStoreEntryRef));
            }
        });
        selected.subscribe(cv -> {
            if (cv == null || cv.getRef() == null) {
                popoverProp.setValue(null);
            }
        });

        this.popover = new StoreChoicePopover<>(
                self, popoverProp, storeClass, applicableCheck, initialCategory, "selectConnection");

        this.selected.addListener((v, o, n) -> {
            TrackEvent.withTrace("Store combo choice value changed")
                    .tag("value", n)
                    .handle();
        });
    }

    private String toName(DataStoreEntry entry) {
        if (entry == null) {
            return null;
        }

        var converted = stringConverter != null ? stringConverter.apply(entry.getStore().asNeeded()) : null;
        var convertedString = converted != null ? " [" + converted + "]" : "";
        return entry.getName() + convertedString;
    }

    @Override
    protected Region createSimple() {
        var combo = new ComboBox<String>();

        ((Region) popover.getPopover().getContentNode()).setMaxHeight(350);
        var skin = new ComboBoxListViewSkin<>(combo) {
            @Override
            public void show() {
                popover.show(combo);
            }

            @Override
            public void hide() {
                popover.hide();
            }
        };
        popover.getPopover().showingProperty().addListener((o, oldValue, newValue) -> {
            if (!newValue) {
                combo.hide();
            }
        });
        combo.setSkin(skin);
        combo.setMaxWidth(20000);
        combo.setEditable(true);

        combo.getEditor().focusedProperty().subscribe(f -> {
            if (f) {
                Platform.runLater(() -> {
                    combo.getEditor().selectAll();
                });
            }
        });

        combo.setValue(selected.getValue() != null ? selected.getValue().getManualHost() : null);

        var internalUpdate = new SimpleBooleanProperty();
        combo.getEditor().textProperty().addListener((v, o, n) -> {
            if (internalUpdate.get()) {
                return;
            }

            selected.setValue(n != null && !n.isEmpty() ? new ComboValue<>(n, null) : null);
        });

        selected.subscribe((v) -> {
            if (v != null && v.getRef() != null) {
                PlatformThread.runLaterIfNeeded(() -> {
                    internalUpdate.set(true);
                    combo.setValue(toName(v.getRef().get()));
                    if (combo.getValue() != null) {
                        combo.getItems().setAll(combo.getValue());
                    } else {
                        combo.getItems().clear();
                    }
                    internalUpdate.set(false);
                });
            }
        });

        return combo;
    }
}
