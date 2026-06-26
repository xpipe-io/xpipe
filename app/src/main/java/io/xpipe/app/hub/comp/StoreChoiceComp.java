package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class StoreChoiceComp<T extends DataStore> extends SimpleRegionBuilder {

    private final ObjectProperty<DataStoreEntryRef<T>> selected;

    private StoreChoicePopover<T> popover;

    public StoreChoiceComp(
            DataStoreEntry self,
            ObjectProperty<DataStoreEntryRef<T>> selected,
            Class<?> storeClass,
            Predicate<DataStoreEntryRef<T>> applicableCheck,
            StoreCategoryWrapper categoryRoot,
            boolean requireComplete,
            DataStoreCreationCategory creationCategory) {
        this(self, selected, storeClass, applicableCheck, categoryRoot, null, requireComplete, creationCategory);
    }

    public StoreChoiceComp(
            DataStoreEntry self,
            ObjectProperty<DataStoreEntryRef<T>> selected,
            Class<?> storeClass,
            Predicate<DataStoreEntryRef<T>> applicableCheck,
            StoreCategoryWrapper categoryRoot,
            StoreCategoryWrapper explicitCategory,
            boolean requireComplete,
            DataStoreCreationCategory creationCategory) {
        this.selected = selected;
        this.popover = new StoreChoicePopover<>(
                self,
                selected,
                storeClass,
                applicableCheck,
                categoryRoot,
                explicitCategory,
                requireComplete,
                StoreViewState.get().getAllConnectionsCategory().equals(categoryRoot)
                        ? "selectConnection"
                        : "selectEntry",
                "noCompatibleConnection",
                creationCategory);
    }

    public StoreChoiceComp(
            DataStoreEntry self,
            ObjectProperty<DataStoreEntryRef<T>> selected,
            Class<?> storeClass,
            Predicate<DataStoreEntryRef<T>> applicableCheck,
            StoreCategoryWrapper initialCategory,
            DataStoreCreationCategory creationCategory) {
        this(self, selected, storeClass, applicableCheck, initialCategory, true, creationCategory);
    }

    protected String toName(DataStoreEntry entry) {
        if (entry == null) {
            return null;
        }

        return DataStorage.get().getStoreEntryDisplayName(entry);
    }

    protected String toGraphic(DataStoreEntry entry) {
        if (entry == null) {
            return null;
        }

        return entry.getEffectiveIconFile();
    }

    @Override
    protected Region createSimple() {
        var button = new ButtonComp(
                Bindings.createStringBinding(
                        () -> {
                            var val = selected.getValue();
                            return toName(val != null ? val.get() : null);
                        },
                        selected),
                () -> {});
        button.describe(d -> d.name(Bindings.createStringBinding(
                () -> {
                    return selected.getValue() != null
                            ? toName(selected.getValue().get())
                            : AppI18n.get("selectConnection");
                },
                selected,
                AppI18n.activeLanguage())));
        button.apply(struc -> {
                    struc.setMaxWidth(20000);
                    struc.setAlignment(Pos.CENTER_LEFT);
                    BaseRegionBuilder<?, ?> graphic = PrettyImageHelper.ofFixedSize(
                            Bindings.createStringBinding(
                                    () -> {
                                        return toGraphic(
                                                selected.getValue() != null
                                                        ? selected.getValue().get()
                                                        : null);
                                    },
                                    selected),
                            16,
                            16);
                    struc.setGraphic(graphic.build());
                    struc.setOnAction(event -> {
                        popover.show(struc);
                        event.consume();
                    });
                    struc.setOnMouseClicked(event -> {
                        if (event.getButton() != MouseButton.SECONDARY) {
                            return;
                        }

                        selected.setValue(null);
                        event.consume();
                    });
                })
                .style("choice-comp")
                .style(Styles.LEFT_PILL);

        var r = button.build();

        var dropdownIcon = new FontIcon("mdal-keyboard_arrow_down");
        dropdownIcon.setDisable(true);
        dropdownIcon.setPickOnBounds(false);
        dropdownIcon.visibleProperty().bind(r.disabledProperty().not());
        AppFontSizes.xl(dropdownIcon);

        var pane = new AnchorPane(r, dropdownIcon);
        r.prefHeightProperty().bind(pane.heightProperty());
        pane.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                r.requestFocus();
            }
        });
        pane.heightProperty().subscribe(number -> {
            AnchorPane.setTopAnchor(dropdownIcon, number.doubleValue() / 3.3);
        });
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
        clearButton.describe(d -> d.nameKey("clear"));
        clearButton.style(Styles.FLAT);
        clearButton.hide(selected.isNull().or(pane.disabledProperty()));
        clearButton.apply(struc -> {
            struc.setOpacity(0.7);
            struc.getStyleClass().add("clear-button");
            AppFontSizes.xs(struc);
            AnchorPane.setRightAnchor(struc, 30.0);
            AnchorPane.setTopAnchor(struc, 3.0);
            AnchorPane.setBottomAnchor(struc, 3.0);
        });
        pane.getChildren().add(clearButton.build());

        var editGraphic = new LabelGraphic.IconGraphic("mdal-edit");
        var editButton = new ButtonComp(null, editGraphic, () -> {
            var ref = selected.getValue();
            StoreCreationDialog.showEdit(ref.get());
        });
        editButton.disable(Bindings.createBooleanBinding(() -> {
            return selected.getValue() == null || selected.get().get().getProvider() == null || !selected.get().get().getProvider().canConfigure();
        }, selected));
        editButton.describe(d -> d.nameKey("edit"));

        var box = new InputGroupComp(List.of(RegionBuilder.of(() -> pane).hgrow(), editButton));
        box.setMainReference(0);
        box.style("store-choice-comp");
        return box.build();
    }
}
