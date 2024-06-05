package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.CountComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FilterComp;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
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

public class StoreEntryListStatusComp extends SimpleComp {

    private final Property<StoreSortMode> sortMode;

    public StoreEntryListStatusComp() {
        this.sortMode = new SimpleObjectProperty<>();
        StoreViewState.get().getActiveCategory().subscribe(val -> {
            sortMode.setValue(val.getSortMode().getValue());
        });
        sortMode.addListener((observable, oldValue, newValue) -> {
            var cat = StoreViewState.get().getActiveCategory().getValue();
            if (cat == null) {
                return;
            }

            cat.getSortMode().setValue(newValue);
        });
    }

    private Region createGroupListHeader() {
        var label = new Label();
        var name = BindingsHelper.flatMap(
                StoreViewState.get().getActiveCategory(),
                categoryWrapper -> AppI18n.observable(
                        categoryWrapper.getRoot().equals(StoreViewState.get().getAllConnectionsCategory())
                                ? "connections"
                                : "scripts"));
        label.textProperty().bind(name);
        label.getStyleClass().add("name");

        var all = StoreViewState.get().getAllEntries().filtered(
                storeEntryWrapper -> {
                    var rootCategory = storeEntryWrapper.getCategory().getValue().getRoot();
                    var inRootCategory = StoreViewState.get().getActiveCategory().getValue().getRoot().equals(rootCategory);
                    // Sadly the all binding does not update when the individual visibility of entries changes
                    // But it is good enough.
                    var showProvider = !storeEntryWrapper.getEntry().getValidity().isUsable() ||
                            storeEntryWrapper.getEntry().getProvider().shouldShow(storeEntryWrapper);
                    return inRootCategory && showProvider;
                },
                StoreViewState.get().getActiveCategory());
        var shownList = all.filtered(
                storeEntryWrapper -> {
                    return storeEntryWrapper.shouldShow(
                            StoreViewState.get().getFilterString().getValue());
                },
                StoreViewState.get().getFilterString());
        var count = new CountComp<>(shownList.getList(), all.getList());

        var c = count.createRegion();
        var topBar = new HBox(
                label,
                c,
                Comp.hspacer().createRegion(),
                createDateSortButton().createRegion(),
                Comp.hspacer(2).createRegion(),
                createAlphabeticalSortButton().createRegion());
        AppFont.setSize(label, 3);
        AppFont.setSize(c, 3);
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
        filter.apply(struc -> struc.get().sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                struc.getText().requestFocus();
            }
        }));

        var f = filter.createRegion();
        var hbox = new HBox(createButtons(), f);
        hbox.setSpacing(8);
        hbox.setAlignment(Pos.CENTER);
        HBox.setHgrow(f, Priority.ALWAYS);

        f.getStyleClass().add("filter-bar");
        if (OsType.getLocal().equals(OsType.MACOS)) {
            f.setPadding(new Insets(-2, 0, -2, 0));
        } else {
            f.setPadding(new Insets(-3, 0, -3, 0));
        }

        AppFont.medium(hbox);
        return hbox;
    }

    private Region createButtons() {
        var menu = new MenuButton(null, new FontIcon("mdi2p-plus-thick"));
        menu.textProperty().bind(AppI18n.observable("addConnections"));
        menu.setAlignment(Pos.CENTER);
        menu.setTextAlignment(TextAlignment.CENTER);
        AppFont.medium(menu);
        StoreCreationMenu.addButtons(menu);
        menu.setOpacity(0.85);
        menu.setMinWidth(Region.USE_PREF_SIZE);

        if (OsType.getLocal().equals(OsType.MACOS)) {
            menu.setPadding(new Insets(-2, 0, -2, 0));
        } else {
            menu.setPadding(new Insets(-3, 0, -3, 0));
        }

        return menu;
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
        alphabetical.accessibleTextKey("sortAlphabetical");
        alphabetical.tooltipKey("sortAlphabetical");
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
