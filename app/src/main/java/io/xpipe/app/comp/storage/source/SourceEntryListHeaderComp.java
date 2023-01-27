package io.xpipe.app.comp.storage.source;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.CountComp;
import io.xpipe.app.comp.source.GuiDsCreatorMultiStep;
import io.xpipe.app.comp.storage.collection.SourceCollectionSortMode;
import io.xpipe.app.comp.storage.collection.SourceCollectionViewState;
import io.xpipe.app.comp.storage.collection.SourceCollectionWrapper;
import io.xpipe.app.core.AppFont;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.extension.fxcomps.impl.HorizontalComp;
import io.xpipe.extension.fxcomps.impl.IconButtonComp;
import io.xpipe.extension.fxcomps.impl.VerticalComp;
import io.xpipe.extension.fxcomps.util.BindingsHelper;
import javafx.beans.binding.Bindings;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class SourceEntryListHeaderComp extends SimpleComp {

    private final SourceCollectionWrapper group;

    public SourceEntryListHeaderComp(SourceCollectionWrapper group) {
        this.group = group;
    }

    private Comp<?> createAlphabeticalSortButton() {
        var icon = Bindings.createStringBinding(
                () -> {
                    if (group.getSortMode() == SourceCollectionSortMode.ALPHABETICAL_ASC) {
                        return "mdi2s-sort-alphabetical-descending";
                    }
                    if (group.getSortMode() == SourceCollectionSortMode.ALPHABETICAL_DESC) {
                        return "mdi2s-sort-alphabetical-ascending";
                    }
                    return "mdi2s-sort-alphabetical-descending";
                },
                group.sortModeProperty());
        var alphabetical = new IconButtonComp(icon, () -> {
            if (group.getSortMode() == SourceCollectionSortMode.ALPHABETICAL_ASC) {
                group.sortModeProperty().setValue(SourceCollectionSortMode.ALPHABETICAL_DESC);
            } else if (group.getSortMode() == SourceCollectionSortMode.ALPHABETICAL_DESC) {
                group.sortModeProperty().setValue(SourceCollectionSortMode.ALPHABETICAL_ASC);
            } else {
                group.sortModeProperty().setValue(SourceCollectionSortMode.ALPHABETICAL_ASC);
            }
        });
        alphabetical.apply(alphabeticalR -> {
            alphabeticalR
                    .get()
                    .opacityProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                if (group.getSortMode() == SourceCollectionSortMode.ALPHABETICAL_ASC
                                        || group.getSortMode() == SourceCollectionSortMode.ALPHABETICAL_DESC) {
                                    return 1.0;
                                }
                                return 0.4;
                            },
                            group.sortModeProperty()));
        });
        alphabetical.apply(new FancyTooltipAugment<>("sortAlphabetical"));
        alphabetical.shortcut(new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN));
        return alphabetical;
    }

    private Comp<?> createDateSortButton() {
        var icon = Bindings.createStringBinding(
                () -> {
                    if (group.getSortMode() == SourceCollectionSortMode.DATE_ASC) {
                        return "mdi2s-sort-clock-ascending-outline";
                    }
                    if (group.getSortMode() == SourceCollectionSortMode.DATE_DESC) {
                        return "mdi2s-sort-clock-descending-outline";
                    }
                    return "mdi2s-sort-clock-ascending-outline";
                },
                group.sortModeProperty());
        var date = new IconButtonComp(icon, () -> {
            if (group.getSortMode() == SourceCollectionSortMode.DATE_ASC) {
                group.sortModeProperty().setValue(SourceCollectionSortMode.DATE_DESC);
            } else if (group.getSortMode() == SourceCollectionSortMode.DATE_DESC) {
                group.sortModeProperty().setValue(SourceCollectionSortMode.DATE_ASC);
            } else {
                group.sortModeProperty().setValue(SourceCollectionSortMode.DATE_ASC);
            }
        });
        date.apply(dateR -> {
            dateR.get()
                    .opacityProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                if (group.getSortMode() == SourceCollectionSortMode.DATE_ASC
                                        || group.getSortMode() == SourceCollectionSortMode.DATE_DESC) {
                                    return 1.0;
                                }
                                return 0.4;
                            },
                            group.sortModeProperty()));
        });
        date.apply(new FancyTooltipAugment<>("sortLastUsed"));
        date.shortcut(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN));
        return date;
    }

    private Comp<?> createListDisplayModeButton() {
        var list = new IconButtonComp("mdi2f-format-list-bulleted-type", () -> {
            group.displayModeProperty().setValue(SourceEntryDisplayMode.LIST);
        });
        list.apply(dateR -> {
            dateR.get()
                    .opacityProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                if (group.getDisplayMode() == SourceEntryDisplayMode.LIST) {
                                    return 1.0;
                                }
                                return 0.4;
                            },
                            group.displayModeProperty()));
        });
        list.apply(new FancyTooltipAugment<>("displayList"));
        return list;
    }

    private Comp<?> createTilesDisplayModeButton() {
        var tiles = new IconButtonComp("mdal-apps", () -> {
            group.displayModeProperty().setValue(SourceEntryDisplayMode.TILES);
        });
        tiles.apply(dateR -> {
            dateR.get()
                    .opacityProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                if (group.getDisplayMode() == SourceEntryDisplayMode.TILES) {
                                    return 1.0;
                                }
                                return 0.4;
                            },
                            group.displayModeProperty()));
        });
        tiles.apply(new FancyTooltipAugment<>("displayTiles"));
        return tiles;
    }

    private Comp<?> createSortButtonBar() {
        return new HorizontalComp(List.of(createDateSortButton(), createAlphabeticalSortButton()));
    }

    private Comp<?> createDisplayModeButtonBar() {
        return new HorizontalComp(List.of(createListDisplayModeButton(), createTilesDisplayModeButton()));
    }

    private Comp<?> createRightButtons() {
        var v = new VerticalComp(List.of(
                createDisplayModeButtonBar().apply(struc -> struc.get().setVisible(false)),
                Comp.of(() -> {
                    return new StackPane(new Separator(Orientation.HORIZONTAL));
                }),
                createSortButtonBar()));
        v.apply(r -> {
                    var sep = r.get().getChildren().get(1);
                    VBox.setVgrow(sep, Priority.ALWAYS);
                })
                .apply(s -> {
                    s.get()
                            .visibleProperty()
                            .bind(BindingsHelper.persist(Bindings.greaterThan(
                                    Bindings.size(
                                            SourceCollectionViewState.get().getAllEntries()),
                                    0)));
                });
        return v;
    }

    @Override
    protected Region createSimple() {
        var label = new Label(I18n.get("none"));
        if (SourceCollectionViewState.get().getSelectedGroup() != null) {
            label.textProperty()
                    .bind(SourceCollectionViewState.get().getSelectedGroup().nameProperty());
        }
        label.getStyleClass().add("name");
        SourceCollectionViewState.get().selectedGroupProperty().addListener((c, o, n) -> {
            if (n != null) {
                label.textProperty().bind(n.nameProperty());
            }
        });
        var count = new CountComp<>(
                SourceCollectionViewState.get().getShownEntries(),
                SourceCollectionViewState.get().getAllEntries());
        var close = new IconButtonComp("mdi2a-arrow-collapse-left", () -> SourceCollectionViewState.get()
                        .selectedGroupProperty()
                        .set(null))
                .createRegion();
        AppFont.medium(close);

        var leftSep = new StackPane(new Separator(Orientation.HORIZONTAL));
        var top = new HBox(label);
        if (group != null) {
            top.getChildren().add(0, close);
            top.getChildren().addAll(count.createRegion());
        }

        top.setAlignment(Pos.CENTER_LEFT);
        top.setSpacing(3);
        var left = new VBox(top, leftSep, createActionsButtonBar().createRegion());
        VBox.setVgrow(leftSep, Priority.ALWAYS);
        var rspacer = new Region();
        HBox.setHgrow(rspacer, Priority.ALWAYS);
        var topBar = new HBox(left, rspacer);
        if (group != null) {
            var right = createRightButtons().createRegion();
            topBar.getChildren().addAll(right);
        }
        topBar.setFillHeight(true);
        topBar.setSpacing(13);
        topBar.getStyleClass().add("top");
        topBar.setAlignment(Pos.CENTER);
        AppFont.header(topBar);

        topBar.getStyleClass().add("bar");
        topBar.getStyleClass().add("entry-bar");
        return topBar;
    }

    private Comp<?> createActionsButtonBar() {
        var newFile = new ButtonComp(
                        I18n.observable(group != null ? "addStream" : "pipeStream"),
                        new FontIcon("mdi2f-file-plus-outline"),
                        () -> {
                            var selected = SourceCollectionViewState.get()
                                    .selectedGroupProperty()
                                    .get();
                            GuiDsCreatorMultiStep.showCreation(
                                    DataSourceProvider.Category.STREAM,
                                    selected != null ? selected.getCollection() : null);
                        })
                .shortcut(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN))
                .apply(new FancyTooltipAugment<>("addStreamDataSource"));

        var newDb = new ButtonComp(
                        I18n.observable(group != null ? "addDatabase" : "pipeDatabase"),
                        new FontIcon("mdi2d-database-plus-outline"),
                        () -> {
                            var selected = SourceCollectionViewState.get()
                                    .selectedGroupProperty()
                                    .get();
                            GuiDsCreatorMultiStep.showCreation(
                                    DataSourceProvider.Category.DATABASE,
                                    selected != null ? selected.getCollection() : null);
                        })
                .shortcut(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN))
                .apply(new FancyTooltipAugment<>("addDatabaseDataSource"));

        //        var newStructure = new IconButton("mdi2b-beaker-plus-outline", () -> {
        //            GuiDsCreatorMultiStep.show(StorageViewState.get().selectedGroupProperty().get(),
        // DataSourceType.STRUCTURE);
        //        }).apply(JfxHelper.apply(new FancyTooltipAugment<>("addStructureDataSource")));
        //
        //        var newText = new IconButton("mdi2t-text-box-plus-outline", () -> {
        //            GuiDsCreatorMultiStep.show(StorageViewState.get().selectedGroupProperty().get(),
        // DataSourceType.TEXT);
        //        }).apply(JfxHelper.apply(new FancyTooltipAugment<>("addTextDataSource")));
        //
        //        var newBinary = new IconButton("mdi2c-card-plus-outline", () -> {
        //            GuiDsCreatorMultiStep.show(StorageViewState.get().selectedGroupProperty().get(),
        // DataSourceType.RAW);
        //        }).apply(JfxHelper.apply(new FancyTooltipAugment<>("addBinaryDataSource")));
        //
        //        var newCollection = new IconButton("mdi2b-briefcase-plus-outline", () -> {
        //            GuiDsCreatorMultiStep.show(StorageViewState.get().selectedGroupProperty().get(),
        // DataSourceType.COLLECTION);
        //        }).apply(JfxHelper.apply(new FancyTooltipAugment<>("addCollectionDataSource")));

        var spaceOr = new Region();
        spaceOr.setPrefWidth(12);
        var box = new HorizontalComp(List.of(newFile, Comp.of(() -> spaceOr), newDb));
        box.apply(s -> AppFont.normal(s.get()));
        return box;
    }
}
