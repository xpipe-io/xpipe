package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.CountComp;
import io.xpipe.app.comp.base.FilterComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.core.OsType;

import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Function;

public class StoreEntryListOverviewComp extends SimpleComp {

    private Region createGroupListHeader() {
        var label = new Label();
        var name = BindingsHelper.flatMap(
                StoreViewState.get().getActiveCategory(),
                categoryWrapper -> AppI18n.observable(
                        categoryWrapper.getRoot().equals(StoreViewState.get().getAllConnectionsCategory())
                                ? "connections"
                                : categoryWrapper
                                                .getRoot()
                                                .equals(StoreViewState.get().getAllScriptsCategory())
                                        ? "scripts"
                                        : "identities"));
        label.textProperty().bind(name);
        label.getStyleClass().add("name");

        var allCount = StoreViewState.get()
                .entriesCount(
                        storeEntryWrapper -> {
                            var rootCategory =
                                    storeEntryWrapper.getCategory().getValue().getRoot();
                            var inRootCategory = StoreViewState.get()
                                    .getActiveCategory()
                                    .getValue()
                                    .getRoot()
                                    .equals(rootCategory);
                            return inRootCategory;
                        },
                        StoreViewState.get().getActiveCategory());
        var count = new CountComp(allCount, allCount, Function.identity());

        var c = count.createRegion();
        var sep = new Separator(Orientation.VERTICAL);
        sep.setPadding(new Insets(6, 3, 4, 3));
        var topBar = new HBox(
                label,
                c,
                Comp.hspacer().createRegion(),
                createIndexSortButton().createRegion(),
                sep,
                createDateSortButton().createRegion(),
                Comp.hspacer(2).createRegion(),
                createAlphabeticalSortButton().createRegion());
        if (OsType.ofLocal() == OsType.MACOS) {
            AppFontSizes.xxxl(label);
            AppFontSizes.xxxl(c);
        } else {
            AppFontSizes.xxl(label);
            AppFontSizes.xxl(c);
        }
        topBar.setAlignment(Pos.CENTER);
        topBar.getStyleClass().add("top");
        return topBar;
    }

    private Region createGroupListFilter() {
        var filter = new FilterComp(StoreViewState.get().getFilterString()).createRegion();
        var add = createAddButton();
        var batchMode = createBatchModeButton().createRegion();
        var hbox = new HBox(add, filter, batchMode);
        filter.minHeightProperty().bind(add.heightProperty());
        filter.prefHeightProperty().bind(add.heightProperty());
        filter.maxHeightProperty().bind(add.heightProperty());
        batchMode.minHeightProperty().bind(add.heightProperty());
        batchMode.prefHeightProperty().bind(add.heightProperty());
        batchMode.maxHeightProperty().bind(add.heightProperty());
        batchMode.minWidthProperty().bind(add.heightProperty());
        batchMode.prefWidthProperty().bind(add.heightProperty());
        batchMode.maxWidthProperty().bind(add.heightProperty());
        hbox.setSpacing(8);
        hbox.setAlignment(Pos.CENTER);
        HBox.setHgrow(filter, Priority.ALWAYS);

        filter.getStyleClass().add("filter-bar");
        return hbox;
    }

    private Region createAddButton() {
        var menu = new MenuButton(null, new FontIcon("mdi2p-plus-thick"));
        menu.setOnShowing(event -> {
            menu.getItems().clear();
            StoreCreationMenu.addButtons(menu, true);
            event.consume();
        });
        menu.textProperty().bind(AppI18n.observable("new"));
        menu.setAlignment(Pos.CENTER);
        menu.setTextAlignment(TextAlignment.CENTER);
        menu.setOpacity(0.85);
        menu.setMinWidth(Region.USE_PREF_SIZE);
        menu.getStyleClass().add("creation-menu");
        return menu;
    }

    private Comp<?> createBatchModeButton() {
        var batchMode = StoreViewState.get().getBatchMode();
        var b = new IconButtonComp("mdi2l-layers", () -> {
            batchMode.setValue(!batchMode.getValue());
        });
        b.styleClass("batch-mode-button");
        b.apply(struc -> {
            batchMode.subscribe(a -> {
                struc.get().pseudoClassStateChanged(PseudoClass.getPseudoClass("active"), a);
            });
            struc.get().getStyleClass().remove(Styles.FLAT);
        });
        return b;
    }

    private Comp<?> createIndexSortButton() {
        var sortMode = StoreViewState.get().getGlobalSortMode();
        var icon = Bindings.createObjectBinding(
                () -> {
                    if (sortMode.getValue() == StoreSectionSortMode.INDEX_ASC) {
                        return new LabelGraphic.IconGraphic("mdi2o-order-numeric-ascending");
                    }
                    if (sortMode.getValue() == StoreSectionSortMode.INDEX_DESC) {
                        return new LabelGraphic.IconGraphic("mdi2o-order-numeric-descending");
                    }
                    return new LabelGraphic.IconGraphic("mdi2o-order-numeric-ascending");
                },
                sortMode);
        var button = new IconButtonComp(icon, () -> {
            if (sortMode.getValue() == StoreSectionSortMode.INDEX_ASC) {
                sortMode.setValue(StoreSectionSortMode.INDEX_DESC);
            } else {
                sortMode.setValue(StoreSectionSortMode.INDEX_ASC);
            }
        });
        button.apply(struc -> {
            struc.get()
                    .opacityProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                if (sortMode.getValue() == StoreSectionSortMode.INDEX_ASC
                                        || sortMode.getValue() == StoreSectionSortMode.INDEX_DESC) {
                                    return 1.0;
                                }
                                return 0.4;
                            },
                            sortMode));
        });
        button.accessibleTextKey("sortIndexed");
        button.tooltipKey("sortIndexed");
        return button;
    }

    private Comp<?> createAlphabeticalSortButton() {
        var sortMode = StoreViewState.get().getTieSortMode();
        var icon = Bindings.createObjectBinding(
                () -> {
                    if (sortMode.getValue() == StoreSectionSortMode.ALPHABETICAL_ASC) {
                        return new LabelGraphic.IconGraphic("mdi2o-order-alphabetical-descending");
                    }
                    if (sortMode.getValue() == StoreSectionSortMode.ALPHABETICAL_DESC) {
                        return new LabelGraphic.IconGraphic("mdi2o-order-alphabetical-ascending");
                    }
                    return new LabelGraphic.IconGraphic("mdi2o-order-alphabetical-descending");
                },
                sortMode);
        var alphabetical = new IconButtonComp(icon, () -> {
            if (sortMode.getValue() == StoreSectionSortMode.ALPHABETICAL_ASC) {
                sortMode.setValue(StoreSectionSortMode.ALPHABETICAL_DESC);
            } else if (sortMode.getValue() == StoreSectionSortMode.ALPHABETICAL_DESC) {
                sortMode.setValue(StoreSectionSortMode.ALPHABETICAL_ASC);
            } else {
                sortMode.setValue(StoreSectionSortMode.ALPHABETICAL_ASC);
            }
        });
        alphabetical.apply(alphabeticalR -> {
            alphabeticalR
                    .get()
                    .opacityProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                if (sortMode.getValue() == StoreSectionSortMode.ALPHABETICAL_ASC
                                        || sortMode.getValue() == StoreSectionSortMode.ALPHABETICAL_DESC) {
                                    return 1.0;
                                }
                                return 0.4;
                            },
                            sortMode));
        });
        alphabetical.accessibleTextKey("sortAlphabetical");
        alphabetical.tooltipKey("sortAlphabetical");
        return alphabetical;
    }

    private Comp<?> createDateSortButton() {
        var sortMode = StoreViewState.get().getTieSortMode();
        var icon = Bindings.createObjectBinding(
                () -> {
                    if (sortMode.getValue() == StoreSectionSortMode.DATE_ASC) {
                        return new LabelGraphic.IconGraphic("mdi2s-sort-clock-ascending-outline");
                    }
                    if (sortMode.getValue() == StoreSectionSortMode.DATE_DESC) {
                        return new LabelGraphic.IconGraphic("mdi2s-sort-clock-descending-outline");
                    }
                    return new LabelGraphic.IconGraphic("mdi2s-sort-clock-ascending-outline");
                },
                sortMode);
        var date = new IconButtonComp(icon, () -> {
            if (sortMode.getValue() == StoreSectionSortMode.DATE_ASC) {
                sortMode.setValue(StoreSectionSortMode.DATE_DESC);
            } else if (sortMode.getValue() == StoreSectionSortMode.DATE_DESC) {
                sortMode.setValue(StoreSectionSortMode.DATE_ASC);
            } else {
                sortMode.setValue(StoreSectionSortMode.DATE_ASC);
            }
        });
        date.apply(dateR -> {
            dateR.get()
                    .opacityProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                if (sortMode.getValue() == StoreSectionSortMode.DATE_ASC
                                        || sortMode.getValue() == StoreSectionSortMode.DATE_DESC) {
                                    return 1.0;
                                }
                                return 0.4;
                            },
                            sortMode));
        });
        date.accessibleTextKey("sortLastUsed");
        date.tooltipKey("sortLastUsed");
        return date;
    }

    @Override
    public Region createSimple() {
        var bar = new VBox(createGroupListHeader(), createGroupListFilter());
        bar.setFillWidth(true);
        bar.getStyleClass().add("bar");
        bar.getStyleClass().add("store-header-bar");
        return bar;
    }
}
