package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Predicate;

@RequiredArgsConstructor
public class StoreChoiceComp<T extends DataStore> extends SimpleComp {

    private final Mode mode;
    private final Property<DataStoreEntryRef<T>> selected;

    private final StoreChoicePopover<T> popover;

    public StoreChoiceComp(
            Mode mode,
            DataStoreEntry self,
            Property<DataStoreEntryRef<T>> selected,
            Class<?> storeClass,
            Predicate<DataStoreEntryRef<T>> applicableCheck,
            StoreCategoryWrapper initialCategory) {

        this.mode = mode;

        this.selected = selected;

        this.popover = new StoreChoicePopover<>(
                self, selected, storeClass, applicableCheck, initialCategory, "selectConnection", "noCompatibleConnection");
    }

    public static <T extends DataStore> StoreChoiceComp<T> other(
            Property<DataStoreEntryRef<T>> selected,
            Class<?> clazz,
            Predicate<DataStoreEntryRef<T>> filter,
            StoreCategoryWrapper initialCategory) {
        return new StoreChoiceComp<>(Mode.OTHER, null, selected, clazz, filter, initialCategory);
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
                    struc.get().setMaxWidth(20000);
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
                    struc.get().setOnMouseClicked(event -> {
                        if (event.getButton() != MouseButton.SECONDARY) {
                            return;
                        }

                        selected.setValue(
                                mode == Mode.PROXY ? DataStorage.get().local().ref() : null);
                        event.consume();
                    });
                })
                .styleClass("choice-comp");

        var r = button.grow(true, false).accessibleText("Select connection").createRegion();
        var icon = new FontIcon("mdal-keyboard_arrow_down");
        icon.setDisable(true);
        icon.setPickOnBounds(false);
        icon.visibleProperty().bind(r.disabledProperty().not());
        AppFontSizes.xl(icon);
        var pane = new StackPane(r, icon);
        pane.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                r.requestFocus();
            }
        });
        StackPane.setMargin(icon, new Insets(10));
        pane.setPickOnBounds(false);
        StackPane.setAlignment(icon, Pos.CENTER_RIGHT);
        pane.setMaxWidth(20000);
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
