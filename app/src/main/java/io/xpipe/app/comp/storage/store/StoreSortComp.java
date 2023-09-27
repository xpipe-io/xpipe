package io.xpipe.app.comp.storage.store;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;

import java.util.List;

public class StoreSortComp extends SimpleComp {

    private final Property<StoreSortMode> sortMode;

    public StoreSortComp() {
        this.sortMode = new SimpleObjectProperty<>();
        SimpleChangeListener.apply(StoreViewState.get().getActiveCategory(), val -> {
            sortMode.unbind();
            sortMode.bindBidirectional(val.getSortMode());
        });
    }

    private Comp<?> createAlphabeticalSortButton() {
        var icon = Bindings.createStringBinding(
                () -> {
                    if (sortMode.getValue() == StoreSortMode.ALPHABETICAL_ASC) {
                        return "mdi2s-sort-alphabetical-descending";
                    }
                    if (sortMode.getValue() == StoreSortMode.ALPHABETICAL_DESC) {
                        return "mdi2s-sort-alphabetical-ascending";
                    }
                    return "mdi2s-sort-alphabetical-descending";
                },
                sortMode);
        var alphabetical = new IconButtonComp(icon, () -> {
            if (sortMode.getValue() == StoreSortMode.ALPHABETICAL_ASC) {
                sortMode.setValue(StoreSortMode.ALPHABETICAL_DESC);
            } else if (sortMode.getValue() == StoreSortMode.ALPHABETICAL_DESC) {
                sortMode.setValue(StoreSortMode.ALPHABETICAL_ASC);
            } else {
                sortMode.setValue(StoreSortMode.ALPHABETICAL_ASC);
            }
        });
        alphabetical.apply(alphabeticalR -> {
            alphabeticalR
                    .get()
                    .opacityProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                if (sortMode.getValue() == StoreSortMode.ALPHABETICAL_ASC
                                        || sortMode.getValue() == StoreSortMode.ALPHABETICAL_DESC) {
                                    return 1.0;
                                }
                                return 0.4;
                            },
                            sortMode));
        });
        alphabetical.apply(new FancyTooltipAugment<>("sortAlphabetical"));
        alphabetical.shortcut(new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN));
        return alphabetical;
    }

    private Comp<?> createDateSortButton() {
        var icon = Bindings.createStringBinding(
                () -> {
                    if (sortMode.getValue() == StoreSortMode.DATE_ASC) {
                        return "mdi2s-sort-clock-ascending-outline";
                    }
                    if (sortMode.getValue() == StoreSortMode.DATE_DESC) {
                        return "mdi2s-sort-clock-descending-outline";
                    }
                    return "mdi2s-sort-clock-ascending-outline";
                },
                sortMode);
        var date = new IconButtonComp(icon, () -> {
            if (sortMode.getValue() == StoreSortMode.DATE_ASC) {
                sortMode.setValue(StoreSortMode.DATE_DESC);
            } else if (sortMode.getValue() == StoreSortMode.DATE_DESC) {
                sortMode.setValue(StoreSortMode.DATE_ASC);
            } else {
                sortMode.setValue(StoreSortMode.DATE_ASC);
            }
        });
        date.apply(dateR -> {
            dateR.get()
                    .opacityProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                if (sortMode.getValue() == StoreSortMode.DATE_ASC
                                        || sortMode.getValue() == StoreSortMode.DATE_DESC) {
                                    return 1.0;
                                }
                                return 0.4;
                            },
                            sortMode));
        });
        date.apply(new FancyTooltipAugment<>("sortLastUsed"));
        date.shortcut(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN));
        return date;
    }

    private Comp<?> createSortButtonBar() {
        return new HorizontalComp(List.of(createDateSortButton(), createAlphabeticalSortButton())).apply(struc -> {
            struc.get().setMinHeight(40);
            struc.get().setPrefHeight(40);
            struc.get().setMaxHeight(40);
        }).styleClass("bar").styleClass("store-sort-bar");
    }

    @Override
    protected Region createSimple() {
        return createSortButtonBar().createRegion();
    }
}
