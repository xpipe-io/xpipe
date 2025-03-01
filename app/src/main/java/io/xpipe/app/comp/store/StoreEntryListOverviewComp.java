package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.CountComp;
import io.xpipe.app.comp.base.FilterComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.OsType;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

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

        var all = StoreViewState.get()
                .getAllEntries()
                .filtered(
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
        var allCount = Bindings.size(all.getList());
        var count = new CountComp(allCount, allCount, Function.identity());

        var c = count.createRegion();
        var topBar = new HBox(
                label,
                c,
                Comp.hspacer().createRegion(),
                createDateSortButton().createRegion(),
                Comp.hspacer(2).createRegion(),
                createAlphabeticalSortButton().createRegion());
        if (OsType.getLocal() == OsType.MACOS) {
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
        var filterProperty = new SimpleStringProperty();
        filterProperty.addListener((observable, oldValue, newValue) -> {
            ThreadHelper.runAsync(() -> {
                StoreViewState.get().getFilterString().setValue(newValue);
            });
        });
        var filter = new FilterComp(StoreViewState.get().getFilterString());
        var f = filter.createRegion();
        var button = createAddButton();
        var hbox = new HBox(button, f);
        f.minHeightProperty().bind(button.heightProperty());
        f.prefHeightProperty().bind(button.heightProperty());
        f.maxHeightProperty().bind(button.heightProperty());
        hbox.setSpacing(8);
        hbox.setAlignment(Pos.CENTER);
        HBox.setHgrow(f, Priority.ALWAYS);

        f.getStyleClass().add("filter-bar");
        return hbox;
    }

    private Region createAddButton() {
        var menu = new MenuButton(null, new FontIcon("mdi2p-plus-thick"));
        menu.textProperty().bind(AppI18n.observable("addConnections"));
        menu.setAlignment(Pos.CENTER);
        menu.setTextAlignment(TextAlignment.CENTER);
        StoreCreationMenu.addButtons(menu);
        menu.setOpacity(0.85);
        menu.setMinWidth(Region.USE_PREF_SIZE);

        if (OsType.getLocal().equals(OsType.MACOS)) {
            menu.setPadding(new Insets(-2, 0, -2, 0));
        } else {
            menu.setPadding(new Insets(-5, -2, -5, -2));
        }

        return menu;
    }

    private Comp<?> createAlphabeticalSortButton() {
        var sortMode = StoreViewState.get().getSortMode();
        var icon = Bindings.createObjectBinding(
                () -> {
                    if (sortMode.getValue() == StoreSortMode.ALPHABETICAL_ASC) {
                        return new LabelGraphic.IconGraphic("mdi2s-sort-alphabetical-descending");
                    }
                    if (sortMode.getValue() == StoreSortMode.ALPHABETICAL_DESC) {
                        return new LabelGraphic.IconGraphic("mdi2s-sort-alphabetical-ascending");
                    }
                    return new LabelGraphic.IconGraphic("mdi2s-sort-alphabetical-descending");
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
        alphabetical.accessibleTextKey("sortAlphabetical");
        alphabetical.tooltipKey("sortAlphabetical");
        return alphabetical;
    }

    private Comp<?> createDateSortButton() {
        var sortMode = StoreViewState.get().getSortMode();
        var icon = Bindings.createObjectBinding(
                () -> {
                    if (sortMode.getValue() == StoreSortMode.DATE_ASC) {
                        return new LabelGraphic.IconGraphic("mdi2s-sort-clock-ascending-outline");
                    }
                    if (sortMode.getValue() == StoreSortMode.DATE_DESC) {
                        return new LabelGraphic.IconGraphic("mdi2s-sort-clock-descending-outline");
                    }
                    return new LabelGraphic.IconGraphic("mdi2s-sort-clock-ascending-outline");
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
