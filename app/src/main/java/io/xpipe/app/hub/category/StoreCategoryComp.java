package io.xpipe.app.hub.category;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.RegionDescriptor;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.list.StoreSectionDrag;
import io.xpipe.app.hub.list.StoreViewState;
import io.xpipe.app.platform.*;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.OsType;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Value
public class StoreCategoryComp extends SimpleRegionBuilder {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass ROOT = PseudoClass.getPseudoClass("root");
    private static final PseudoClass FLAT = PseudoClass.getPseudoClass("flat");
    private static final PseudoClass RECURSIVE = PseudoClass.getPseudoClass("recursive");

    StoreCategoryWrapper category;

    @Override
    protected Region createSimple() {
        var prop = new SimpleStringProperty();
        category.getName().subscribe(prop::setValue);
        AppPrefs.get().censorMode().subscribe(aBoolean -> {
            var n = category.getName().getValue();
            prop.setValue(aBoolean ? "*".repeat(n.length()) : n);
        });
        prop.addListener((observable, oldValue, newValue) -> {
            if (!AppPrefs.get().censorMode().get()) {
                category.getName().setValue(newValue);
            }
        });
        var name = new LazyTextFieldComp(prop)
                .style("name")
                .applyStructure(struc -> {
                    category.getRenameTrigger().onFire(() -> {
                        struc.get().requestFocus();
                        struc.getTextField().selectAll();
                    });
                })
                .build();
        var showing = new SimpleBooleanProperty();

        var expandIcon = Bindings.createObjectBinding(
                () -> {
                    if (category.getChildren().getList().size() == 0) {
                        return new LabelGraphic.IconGraphic("mdal-keyboard_arrow_right");
                    }

                    var exp = category.getExpanded().get();
                    return new LabelGraphic.IconGraphic(
                            exp ? "mdal-keyboard_arrow_down" : "mdi2c-chevron-double-right");
                },
                category.getExpanded(),
                category.getChildren().getList());
        var expandButton = new IconButtonComp(expandIcon, () -> {
                    category.toggleExpanded();
                })
                .apply(struc -> {
                    struc.setAlignment(Pos.CENTER);
                    if (OsType.ofLocal() == OsType.WINDOWS) {
                        HBox.setMargin(struc, new Insets(0, 0, 2.3, 0));
                    } else if (OsType.ofLocal() == OsType.MACOS) {
                        HBox.setMargin(struc, new Insets(0, 0, 1.8, 0));
                    }
                })
                .disable(Bindings.isEmpty(category.getChildren().getList()))
                .style("expand-button")
                .describe(d -> d.nameKey("expand")
                        .shortcut(new KeyCodeCombination(KeyCode.SPACE))
                        .showTooltips(false));

        var focus = new SimpleBooleanProperty();
        var hover = new SimpleBooleanProperty();
        var statusIcon = Bindings.createObjectBinding(
                () -> {
                    if (hover.get() || focus.get()) {
                        return new LabelGraphic.IconGraphic("mdomz-settings");
                    }

                    if (!DataStorage.get().syncEnabled()
                            || (!category.getCategory().canShare())) {
                        return new LabelGraphic.IconGraphic("mdi2g-git");
                    }

                    return new LabelGraphic.IconGraphic(category.getSync().getValue() ? "mdi2g-git" : "mdi2c-cancel");
                },
                category.getSync(),
                hover,
                focus);
        var statusButton = new IconButtonComp(statusIcon)
                .apply(struc -> AppFontSizes.xs(struc))
                .apply(struc -> {
                    struc.setAlignment(Pos.CENTER);
                    struc.setPadding(new Insets(0, 0, 0, 0));
                })
                .apply(new ContextMenuAugment<>(
                        mouseEvent -> mouseEvent.getButton() == MouseButton.PRIMARY, null, () -> {
                            var cm = createContextMenu();
                            showing.bind(cm.showingProperty());
                            return cm;
                        }))
                .describe(d -> d.nameKey("configuration"))
                .style("status-button");

        var count = new CountComp(
                category.getShownContainedEntriesCount(),
                category.getAllContainedEntriesCount(),
                string -> "[" + string + "]");
        count.style("count");
        count.hide(Bindings.equal(0, category.getShownContainedEntriesCount()));
        count.minWidth(Region.USE_PREF_SIZE);

        var orderIndex = new LabelComp(BindingsHelper.map(category.getOrderIndex(), number -> "" + number));
        var p = AppPrefs.get();
        orderIndex.show(Bindings.createBooleanBinding(
                () -> {
                    return p.developerMode().getValue()
                            && p.developerShowOrderIndices().get();
                },
                p.developerShowOrderIndices(),
                p.developerMode()));

        var iconButton = new StoreCategoryIconComp(category, 16);

        var dragOver = new SimpleBooleanProperty();
        var dragIntoIndicator = createDragIntoIndicator(dragOver);

        var showStatus = hover.or(new SimpleBooleanProperty(DataStorage.get().syncEnabled()))
                .or(showing)
                .or(focus);
        var h = new HorizontalComp(List.of(
                RegionBuilder.hspacer((category.getDepth() * 8)),
                expandButton,
                RegionBuilder.hspacer(3),
                iconButton,
                RegionBuilder.hspacer(4),
                RegionBuilder.of(() -> name).hgrow(),
                orderIndex,
                dragIntoIndicator,
                RegionBuilder.hspacer(4),
                count,
                RegionBuilder.hspacer(9),
                statusButton.hide(showStatus.not())));
        h.apply(struc -> struc.setAccessibleRole(AccessibleRole.BUTTON));
        h.style("category-button")
                .apply(struc -> hover.bind(struc.hoverProperty()))
                .apply(struc -> focus.bind(struc.focusWithinProperty()))
                .maxWidth(2000);
        h.describe(b -> b.showTooltips(false)
                .focusTraversal(RegionDescriptor.FocusTraversal.ENABLED)
                .name(category.getName()));
        h.apply(struc -> {
            setupDragHoverClasses(struc);

            struc.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getButton() != MouseButton.PRIMARY) {
                    return;
                }

                category.select();
                mouseEvent.consume();
            });
        });

        h.apply(new ContextMenuAugment<>(
                mouseEvent -> mouseEvent.getButton() == MouseButton.SECONDARY,
                keyEvent -> keyEvent.getCode() == KeyCode.SPACE,
                () -> createContextMenu()));
        h.apply(struc -> {
            struc.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.SPACE) {
                    category.toggleExpanded();
                    event.consume();
                }
            });
        });

        var l = category.getShownChildren().getList();
        var children =
                new ListBoxViewComp<>(l, l, storeCategoryWrapper -> new StoreCategoryComp(storeCategoryWrapper), false);
        children.style("children");
        children.minHeight(0);
        children.setVisibilityControl(true);
        children.setFixScrollReset(true);

        var hide = Bindings.createBooleanBinding(
                () -> {
                    return !category.getExpanded().get()
                            || category.getChildren().getList().isEmpty();
                },
                category.getChildren().getList(),
                category.getExpanded());
        var v = new VerticalComp(
                List.of(createTopDragIndicator(), h, children.hide(hide), createBottomDragIndicator()));
        v.style("category");
        v.apply(struc -> {
            setupDragAndDrop(struc, dragOver);

            struc.pseudoClassStateChanged(ROOT, category.getCategory().getParentCategory() == null);
            StoreViewState.get().getActiveCategory().subscribe(val -> {
                struc.pseudoClassStateChanged(SELECTED, val.equals(category));
            });

            category.getColor().subscribe((c) -> {
                DataStoreColor.applyStyleClasses(c, struc);
            });

            Listeners.subscribeWeak(struc, AppPrefs.get().showChildCategoriesInParentCategory(), (vBox, aBoolean) -> {
                vBox.pseudoClassStateChanged(RECURSIVE, aBoolean);
                vBox.pseudoClassStateChanged(FLAT, !aBoolean);
            });
        });

        return v.build();
    }

    private RegionBuilder<Label> createDragIntoIndicator(BooleanProperty dragOver) {
        var l = new LabelComp(null, new LabelGraphic.IconGraphic("mdi2f-folder-arrow-right-outline"));
        l.style("drag-into-indicator");
        l.show(Bindings.createBooleanBinding(
                () -> {
                    if (!dragOver.get()) {
                        return false;
                    }

                    return StoreViewState.get().getCategoryDragOperation().getValue() != null
                            && (!category.getExpanded().getValue()
                                    || category.getChildren().getList().isEmpty());
                },
                StoreViewState.get().getCategoryDragOperation(),
                category.getExpanded(),
                category.getChildren().getList(),
                dragOver));
        l.apply(r -> {
            r.setOnDragEntered(event -> {
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);

                category.getExpanded().set(true);
            });

            r.setOnDragOver(event -> {
                var categoryOp = StoreViewState.get().getCategoryDragOperation().getValue();
                if (categoryOp != null) {
                    var target = categoryOp.isValidSubTarget(category)
                            ? new StoreCategoryDrag.SubTarget(getCategory())
                            : null;
                    if (target != null) {
                        StoreViewState.get().setCategoryDragTarget(target);
                        event.acceptTransferModes(TransferMode.MOVE);
                        event.consume();
                    }
                }
            });

            r.setOnDragExited(event -> {
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), false);

                var categoryOp = StoreViewState.get().getCategoryDragOperation().getValue();
                if (categoryOp != null) {
                    StoreViewState.get().setCategoryDragTarget(new StoreCategoryDrag.UndeterminedTarget());
                    event.consume();
                }
            });
        });
        return l;
    }

    private RegionBuilder<Region> createTopDragIndicator() {
        return RegionBuilder.of(() -> {
            var top = new Region();
            top.getStyleClass().add("drag-indicator");
            top.pseudoClassStateChanged(PseudoClass.getPseudoClass("dense"), true);

            Listeners.attachWithScene(top, StoreViewState.get().getCategoryDragOperation(), drag -> {
                var active = drag != null
                        && drag.getTarget()
                                .getCategoryTarget()
                                .map(s -> s.equals(category))
                                .orElse(false);
                var topActive = active && drag.getTarget().getOrder() == StoreCategoryDrag.Order.BEFORE;
                top.setVisible(topActive);
            });

            return top;
        });
    }

    private RegionBuilder<Region> createBottomDragIndicator() {
        return RegionBuilder.of(() -> {
            var bottom = new Region();
            bottom.getStyleClass().add("drag-indicator");
            bottom.pseudoClassStateChanged(PseudoClass.getPseudoClass("dense"), true);

            Listeners.attachWithScene(bottom, StoreViewState.get().getCategoryDragOperation(), drag -> {
                var active = drag != null
                        && drag.getTarget()
                                .getCategoryTarget()
                                .map(s -> s.equals(category))
                                .orElse(false);
                var topActive = active && drag.getTarget().getOrder() == StoreCategoryDrag.Order.AFTER;
                bottom.setVisible(topActive);
            });

            return bottom;
        });
    }

    private void setupDragHoverClasses(Region r) {
        r.setOnDragEntered(event -> {
            r.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        });

        r.setOnDragExited(event -> {
            r.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), false);
        });
    }

    private void setupDragAndDrop(Region r, BooleanProperty dragOver) {
        r.setOnDragEntered(event -> {
            dragOver.set(true);
        });

        r.setOnDragDetected(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            if (!category.canDrag()) {
                return;
            }

            StoreViewState.get().startCategoryDrag(category, r);

            event.consume();
        });

        r.setOnDragDropped(event -> {
            var sectionOp = StoreViewState.get().getSectionDragOperation().getValue();
            if (sectionOp != null) {
                sectionOp.getTarget().execute(sectionOp.getSelection());
                StoreViewState.get().stopSectionDrag();
                StoreViewState.get().getBatchModeSelection().getList().clear();
                StoreViewState.get().getActiveCategory().setValue(category);

                event.setDropCompleted(true);
                event.consume();
            }

            var categoryOp = StoreViewState.get().getCategoryDragOperation().getValue();
            if (categoryOp != null) {
                categoryOp.getTarget().execute(categoryOp.getSelection());
                StoreViewState.get().stopCategoryDrag();

                event.setDropCompleted(true);
                event.consume();
            }

            dragOver.set(false);
        });

        r.setOnDragDone(event -> {
            StoreViewState.get().stopCategoryDrag();
            event.consume();
        });

        r.setOnDragExited(event -> {
            dragOver.set(false);

            var sectionOp = StoreViewState.get().getSectionDragOperation().getValue();
            if (sectionOp != null) {
                StoreViewState.get().setSectionDragTarget(new StoreSectionDrag.UndeterminedTarget());
                event.consume();
            }

            var categoryOp = StoreViewState.get().getCategoryDragOperation().getValue();
            if (categoryOp != null) {
                StoreViewState.get().setCategoryDragTarget(new StoreCategoryDrag.UndeterminedTarget());
                event.consume();
            }
        });

        r.setOnDragOver(event -> {
            var sectionOp = StoreViewState.get().getSectionDragOperation().getValue();
            if (sectionOp != null) {
                var valid = sectionOp.getSelection().stream().allMatch(wrapper -> category.canMoveToThis(wrapper));
                if (!valid) {
                    event.consume();
                    return;
                }

                var target = new StoreSectionDrag.CategoryTarget(getCategory());
                StoreViewState.get().setSectionDragTarget(target);
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            }

            var categoryOp = StoreViewState.get().getCategoryDragOperation().getValue();
            if (categoryOp != null) {
                var valid = categoryOp.isValidTarget(category);
                if (!valid) {
                    event.consume();
                    return;
                }

                var order = event.getY() > r.getHeight() / 2.0
                        ? StoreCategoryDrag.Order.AFTER
                        : StoreCategoryDrag.Order.BEFORE;
                var target = new StoreCategoryDrag.CategoryTarget(getCategory(), order);
                StoreViewState.get().setCategoryDragTarget(target);
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            }
        });
    }

    private ContextMenu createContextMenu() {
        var contextMenu = MenuHelper.createContextMenu();

        if (AppPrefs.get().enableHttpApi().get()) {
            var copyId = new MenuItem(AppI18n.get("copyId"), new FontIcon("mdi2c-content-copy"));
            copyId.setOnAction(event ->
                    ClipboardHelper.copyText(category.getCategory().getUuid().toString()));
            contextMenu.getItems().add(copyId);
        }

        if (AppPrefs.get().developerMode().getValue()) {
            var browse = new MenuItem(AppI18n.get("browseInternalStorage"), new FontIcon("mdi2f-folder-open-outline"));
            browse.setOnAction(
                    event -> DesktopHelper.browseFile(category.getCategory().getDirectory()));
            contextMenu.getItems().add(browse);
        }

        var newCategory = new MenuItem(AppI18n.get("createNewCategory"), new FontIcon("mdi2p-plus-thick"));
        newCategory.setOnAction(event -> {
            StoreViewState.get().createNewCategory(category);
        });
        newCategory.setDisable(!DataStorage.get().canCreateStoreCategoryWithin(category.getCategory()));
        contextMenu.getItems().add(newCategory);

        contextMenu.getItems().add(new SeparatorMenuItem());

        var configure = new MenuItem(AppI18n.get("configure"), new FontIcon("mdi2w-wrench-outline"));
        configure.setOnAction(event -> {
            StoreCategoryConfigComp.show(category);
        });
        contextMenu.getItems().add(configure);

        var rename = new MenuItem(AppI18n.get("rename"), new FontIcon("mdal-edit"));
        rename.setOnAction(event -> {
            category.getRenameTrigger().fire(null);
            event.consume();
        });
        contextMenu.getItems().add(rename);

        var color = new Menu(AppI18n.get("color"), new FontIcon("mdi2f-format-color-fill"));
        var none = new MenuItem();
        none.textProperty().bind(AppI18n.observable("none"));
        none.setOnAction(event -> {
            category.updateConfig(category.getCategory().getConfig().withColor(null));
            event.consume();
        });
        none.setGraphic(DataStoreColor.createDisplayGraphic(null));
        color.getItems().add(none);
        Arrays.stream(DataStoreColor.values()).forEach(dataStoreColor -> {
            MenuItem m = new MenuItem();
            m.textProperty().bind(AppI18n.observable(dataStoreColor.getId()));
            m.setOnAction(event -> {
                category.updateConfig(category.getCategory().getConfig().withColor(dataStoreColor));
                event.consume();
            });
            m.setGraphic(DataStoreColor.createDisplayGraphic(dataStoreColor));
            color.getItems().add(m);
        });
        contextMenu.getItems().add(color);

        contextMenu.getItems().add(new SeparatorMenuItem());

        var del = new MenuItem(AppI18n.get("remove"), new FontIcon("mdal-delete_outline"));
        del.setOnAction(event -> {
            category.delete();
        });
        del.setDisable(!DataStorage.get().canDeleteStoreCategory(category.getCategory()));
        contextMenu.getItems().add(del);

        return contextMenu;
    }
}
