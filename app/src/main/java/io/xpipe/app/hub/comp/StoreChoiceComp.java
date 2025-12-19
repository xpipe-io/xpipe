package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Predicate;

@RequiredArgsConstructor
public class StoreChoiceComp<T extends DataStore> extends SimpleComp {

    private final ObjectProperty<DataStoreEntryRef<T>> selected;

    private final StoreChoicePopover<T> popover;

    public StoreChoiceComp(
            DataStoreEntry self,
            ObjectProperty<DataStoreEntryRef<T>> selected,
            Class<?> storeClass,
            Predicate<DataStoreEntryRef<T>> applicableCheck,
            StoreCategoryWrapper initialCategory) {
        this.selected = selected;
        this.popover = new StoreChoicePopover<>(
                self,
                selected,
                storeClass,
                applicableCheck,
                initialCategory,
                "selectConnection",
                "noCompatibleConnection");
    }

    protected String toName(DataStoreEntry entry) {
        if (entry == null) {
            return null;
        }

        return DataStorage.get().getStoreEntryDisplayName(entry);
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
        button.descriptor(d -> d.name(Bindings.createStringBinding(() -> {
            return selected.getValue() != null ? toName(selected.getValue().get()) : AppI18n.get("selectConnection");
        }, selected, AppI18n.activeLanguage())));
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

                        selected.setValue(null);
                        event.consume();
                    });
                })
                .styleClass("choice-comp");

        var r = button.createRegion();

        var dropdownIcon = new FontIcon("mdal-keyboard_arrow_down");
        dropdownIcon.setDisable(true);
        dropdownIcon.setPickOnBounds(false);
        dropdownIcon.visibleProperty().bind(r.disabledProperty().not());
        AppFontSizes.xl(dropdownIcon);

        var pane = new AnchorPane(r, dropdownIcon);
        pane.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                r.requestFocus();
            }
        });
        AnchorPane.setTopAnchor(dropdownIcon, 11.0);
        AnchorPane.setRightAnchor(dropdownIcon, 7.0);
        AnchorPane.setRightAnchor(r, 0.0);
        AnchorPane.setLeftAnchor(r, 0.0);
        pane.setPickOnBounds(false);
        pane.setMaxWidth(20000);

        var clearButton = new IconButtonComp("mdi2c-close", () -> {
            selected.setValue(null);
            Platform.runLater(() -> {
                pane.requestFocus();
            });
        });
        clearButton.styleClass(Styles.FLAT);
        clearButton.hide(selected.isNull().or(pane.disabledProperty()));
        clearButton.apply(struc -> {
            struc.get().setOpacity(0.7);
            struc.get().getStyleClass().add("clear-button");
            AppFontSizes.xs(struc.get());
            AnchorPane.setRightAnchor(struc.get(), 30.0);
            AnchorPane.setTopAnchor(struc.get(), 3.0);
            AnchorPane.setBottomAnchor(struc.get(), 3.0);
        });
        pane.getChildren().add(clearButton.createRegion());

        return pane;
    }
}
