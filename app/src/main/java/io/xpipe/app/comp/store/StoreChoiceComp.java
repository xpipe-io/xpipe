package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.FilterComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.DataStoreCategoryChoiceComp;
import io.xpipe.core.store.DataStore;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class StoreChoiceComp<T extends DataStore> extends SimpleComp {

    private final Mode mode;
    private final Property<DataStoreEntryRef<T>> selected;
    private final StoreChoicePopover<T> popover;

    public StoreChoiceComp(
            Mode mode, DataStoreEntry self, Property<DataStoreEntryRef<T>> selected, Class<T> storeClass,
            Predicate<DataStoreEntryRef<T>> applicableCheck, StoreCategoryWrapper initialCategory
    ) {
        this.mode = mode;
        this.selected = selected;
        this.popover = new StoreChoicePopover<>(self,selected,storeClass, applicableCheck, initialCategory, "selectConnection");
    }

    public static <T extends DataStore> StoreChoiceComp<T> other(
            Property<DataStoreEntryRef<T>> selected,
            Class<T> clazz,
            Predicate<DataStoreEntryRef<T>> filter,
            StoreCategoryWrapper initialCategory) {
        return new StoreChoiceComp<>(Mode.OTHER, null, selected, clazz, filter, initialCategory);
    }

    public static StoreChoiceComp<ShellStore> proxy(
            Property<DataStoreEntryRef<ShellStore>> selected, StoreCategoryWrapper initialCategory) {
        return new StoreChoiceComp<>(Mode.PROXY, null, selected, ShellStore.class, null, initialCategory);
    }

    public static StoreChoiceComp<ShellStore> host(
            Property<DataStoreEntryRef<ShellStore>> selected, StoreCategoryWrapper initialCategory) {
        return new StoreChoiceComp<>(Mode.HOST, null, selected, ShellStore.class, null, initialCategory);
    }

    private String toName(DataStoreEntry entry) {
        if (entry == null) {
            return null;
        }

        if (mode == Mode.PROXY && entry.getStore() instanceof LocalStore) {
            return AppI18n.get("none");
        }

        return entry.getName();
    }

    @Override
    protected Region createSimple() {
        var button = new ButtonComp(
                Bindings.createStringBinding(
                        () -> {
                            var val = selected.getValue();
                            return val != null ? toName(val.get()) : null;
                        },
                        selected),
                () -> {});
        button.apply(struc -> {
                    struc.get().setMaxWidth(2000);
                    struc.get().setAlignment(Pos.CENTER_LEFT);
                    Comp<?> graphic = PrettyImageHelper.ofFixedSize(
                            Bindings.createStringBinding(
                                    () -> {
                                        var val = selected.getValue();
                                        if (val == null) {
                                            return null;
                                        }

                                        return val.get().getEffectiveIconFile();
                                    },
                                    selected),
                            16,
                            16);
                    struc.get().setGraphic(graphic.createRegion());
                    struc.get().setOnAction(event -> {
                        popover.show(struc.get());
                        event.consume();
                    });
                })
                .styleClass("choice-comp");

        var r = button.grow(true, false).accessibleText("Select connection").createRegion();
        var icon = new FontIcon("mdal-keyboard_arrow_down");
        icon.setDisable(true);
        icon.setPickOnBounds(false);
        AppFont.header(icon);
        var pane = new StackPane(r, icon);
        pane.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                r.requestFocus();
            }
        });
        StackPane.setMargin(icon, new Insets(10));
        pane.setPickOnBounds(false);
        StackPane.setAlignment(icon, Pos.CENTER_RIGHT);
        pane.setMaxWidth(2000);
        r.prefWidthProperty().bind(pane.widthProperty());
        r.maxWidthProperty().bind(pane.widthProperty());
        return pane;
    }

    public enum Mode {
        HOST,
        OTHER,
        PROXY
    }
}
