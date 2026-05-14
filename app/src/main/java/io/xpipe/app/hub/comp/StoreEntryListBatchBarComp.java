package io.xpipe.app.hub.comp;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.augment.ContextMenuAugment;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppImages;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.hub.action.BatchHubProvider;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.MenuHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

public class StoreEntryListBatchBarComp extends SimpleRegionBuilder {

    private final BooleanProperty expanded;

    public StoreEntryListBatchBarComp(BooleanProperty expanded) {this.expanded = expanded;}

    @Override
    protected Region createSimple() {
        var checkbox = new StoreEntryBatchSelectComp(StoreViewState.get().getCurrentTopLevelSection());
        var l = new LabelComp(Bindings.createStringBinding(
                () -> {
                    return AppI18n.get(
                            "connectionsSelected",
                            StoreViewState.get()
                                    .getEffectiveBatchModeSelection()
                                    .getList()
                                    .size());
                },
                StoreViewState.get().getEffectiveBatchModeSelection().getList(),
                AppI18n.activeLanguage()));
        l.minWidth(Region.USE_PREF_SIZE);
        l.apply(struc -> {
            struc.setAlignment(Pos.CENTER);
        });
        var actions = new HorizontalComp(createActions());
        actions.spacing(2);

        var close = new IconButtonComp("mdi2c-close", () -> {
            StoreViewState.get().getBatchMode().setValue(false);
        });
        close.describe(d -> d.nameKey("close"));
        close.apply(struc -> {
            struc.getStyleClass().remove(Styles.FLAT);
            struc.minWidthProperty().bind(struc.heightProperty());
            struc.prefWidthProperty().bind(struc.heightProperty());
            struc.maxWidthProperty().bind(struc.heightProperty());
        });

        var toggleExpand = new IconButtonComp("mdi2a-arrow-expand-horizontal", () -> {
            expanded.set(!expanded.get());
        });
        toggleExpand.describe(d -> d.nameKey("expand"));
        toggleExpand.hide(Bindings.isEmpty(StoreViewState.get().getEffectiveBatchModeSelection().getList()));
        toggleExpand.apply(struc -> {
            struc.getStyleClass().remove(Styles.FLAT);
            struc.minWidthProperty().bind(struc.heightProperty());
            struc.prefWidthProperty().bind(struc.heightProperty());
            struc.maxWidthProperty().bind(struc.heightProperty());
        });

        var bar = new HorizontalComp(List.of(
                checkbox,
                RegionBuilder.hspacer(12),
                l,
                RegionBuilder.hspacer(20),
                actions.hgrow(),
                RegionBuilder.hspacer(20),
                toggleExpand,
                close));
        bar.apply(struc -> {
            struc.setFillHeight(true);
            struc.setAlignment(Pos.CENTER_LEFT);
        });
        bar.minHeight(40);
        bar.prefHeight(40);
        bar.style("bar");
        bar.style("store-entry-list-status-bar");
        return bar.build();
    }

    private ObservableList<BaseRegionBuilder<?, ?>> createActions() {
        var actions = DerivedObservableList.<BaseRegionBuilder<?,?>>arrayList(true);
        StoreViewState.get().getBatchModeSelection().getList().addListener((ListChangeListener<
                ? super StoreEntryWrapper>)
                c -> {
                    actions.getList().clear();
                    for (var p : getCompatibleActionProviders()) {
                        actions.getList().add(buildButton(p));
                    }
                    if (c.getList().size() > 0) {
                        actions.getList().add(RegionBuilder.vseparator());
                    }
                    actions.getList().add(RegionBuilder.hspacer());
                    if (c.getList().size() > 0) {
                        actions.getList().add(RegionBuilder.vseparator());
                        actions.getList().add(buildMoveButton());
                        actions.getList().add(buildDeleteButton());
                    }
                });
        return actions.getList();
    }

    private List<ActionProvider> getCompatibleActionProviders() {
        var l = StoreViewState.get().getEffectiveBatchModeSelection().getList();
        if (l.isEmpty()) {
            return List.of();
        }

        var all = new ArrayList<>(ActionProvider.ALL);
        for (StoreEntryWrapper w : l) {
            var actions = ActionProvider.ALL.stream()
                    .filter(actionProvider -> {
                        var s = actionProvider instanceof BatchHubProvider<?> b ? b : null;
                        if (s == null) {
                            return false;
                        }

                        if (!s.getApplicableClass()
                                .isAssignableFrom(w.getStore().getValue().getClass())) {
                            return false;
                        }

                        if (!w.getEntry().getValidity().isUsable() && s.requiresValidStore()) {
                            return false;
                        }

                        if (!s.isApplicable(w.getEntry().ref())) {
                            return false;
                        }

                        return true;
                    })
                    .toList();
            all.removeIf(actionProvider -> !actions.contains(actionProvider));
        }
        return all;
    }

    @SuppressWarnings("unchecked")
    private <T extends DataStore> BaseRegionBuilder<?, ?> buildButton(ActionProvider p) {
        BatchHubProvider<T> s = (BatchHubProvider<T>) p;
        if (s == null) {
            return RegionBuilder.empty();
        }

        var childrenRefs = StoreViewState.get()
                .getEffectiveBatchModeSelection()
                .mapped(storeEntryWrapper -> storeEntryWrapper.getEntry().<T>ref());
        var batchActions = s.getChildren(childrenRefs.getList());
        var name = Bindings.createStringBinding(() -> {
            return expanded.get() ? s.getName().getValue() : null;
        }, expanded, s.getName());
        var button = new ButtonComp(name, new SimpleObjectProperty<>(s.getIcon()), () -> {
            if (batchActions.size() > 0) {
                return;
            }

            runActions(s);
        });
        button.describe(d -> d.name(s.getName()));

        button.disable(Bindings.createBooleanBinding(
                () -> {
                    return childrenRefs.getList().stream().anyMatch(ref -> !s.isActive(ref));
                },
                childrenRefs.getList()));

        if (batchActions.size() > 0) {
            button.apply(new ContextMenuAugment<>(
                    mouseEvent -> mouseEvent.getButton() == MouseButton.PRIMARY, keyEvent -> false, () -> {
                        var cm = MenuHelper.createContextMenu();
                        s.getChildren(childrenRefs.getList()).forEach(childProvider -> {
                            var menu = buildMenuItemForAction(childrenRefs.getList(), childProvider);
                            cm.getItems().add(menu);
                        });
                        return cm;
                    }));
        }
        return button;
    }

    private BaseRegionBuilder<?, ?> buildMoveButton() {
        var i18n = AppI18n.observable("move");
        var name = Bindings.createStringBinding(() -> {
            return expanded.get() ? i18n.getValue() : null;
        }, expanded, i18n);
        var button = new ButtonComp(name, new SimpleObjectProperty<>(new LabelGraphic.IconGraphic("mdi2f-folder-move-outline")), null);
        button.describe(d -> d.name(i18n));
        button.apply(new ContextMenuAugment<>(
                mouseEvent -> mouseEvent.getButton() == MouseButton.PRIMARY, keyEvent -> false, () -> {
            var selection = StoreViewState.get().getBatchModeSelection().getList();
            if (selection.isEmpty()) {
                return null;
            }

            var refWrapper = selection.getFirst();

            var menu = MenuHelper.createContextMenu();
            StoreViewState.get()
                    .getSortedCategories(
                            refWrapper.getCategory().getValue().getRoot(), true)
                    .getList()
                    .stream()
                    .filter(w -> {
                        var isSource = DataStorage.get()
                                .getCategoryParentHierarchy(DataStorage.get()
                                        .getStoreCategory(refWrapper.getEntry()))
                                .stream()
                                .anyMatch(h -> h.getUuid().equals(DataStorage.SCRIPT_SOURCES_CATEGORY_UUID));
                        if (isSource) {
                            return DataStorage.get().getCategoryParentHierarchy(w.getCategory()).stream()
                                    .anyMatch(
                                            h -> h.getUuid().equals(DataStorage.SCRIPT_SOURCES_CATEGORY_UUID));
                        }

                        var isScript = DataStorage.get()
                                .getCategoryParentHierarchy(DataStorage.get()
                                        .getStoreCategory(refWrapper.getEntry()))
                                .stream()
                                .anyMatch(h -> h.getUuid().equals(DataStorage.ALL_SCRIPTS_CATEGORY_UUID));
                        if (isScript) {
                            return DataStorage.get().getCategoryParentHierarchy(w.getCategory()).stream()
                                    .noneMatch(
                                            h -> h.getUuid().equals(DataStorage.SCRIPT_SOURCES_CATEGORY_UUID));
                        }

                        var isLocalIdentity = DataStorage.get()
                                .getCategoryParentHierarchy(DataStorage.get()
                                        .getStoreCategory(refWrapper.getEntry()))
                                .stream()
                                .anyMatch(h -> h.getUuid().equals(DataStorage.LOCAL_IDENTITIES_CATEGORY_UUID));
                        if (isLocalIdentity) {
                            return DataStorage.get().getCategoryParentHierarchy(w.getCategory()).stream()
                                    .noneMatch(
                                            h -> h.getUuid().equals(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID));
                        }

                        var isSyncedIdentity = DataStorage.get()
                                .getCategoryParentHierarchy(DataStorage.get()
                                        .getStoreCategory(refWrapper.getEntry()))
                                .stream()
                                .anyMatch(h -> h.getUuid().equals(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID));
                        if (isSyncedIdentity) {
                            return DataStorage.get().getCategoryParentHierarchy(w.getCategory()).stream()
                                    .noneMatch(
                                            h -> h.getUuid().equals(DataStorage.LOCAL_IDENTITIES_CATEGORY_UUID));
                        }

                        return true;
                    })
                    .forEach(targetCategory -> {
                        var m = new CustomMenuItem();

                        var li = new Label();
                        li.setGraphic(PrettyImageHelper.ofFixedSizeSquare(
                                        targetCategory
                                                .getIconFile()
                                                .getValue(),
                                        16)
                                .padding(new Insets(0, 0, 1, 0))
                                .build());
                        li.setText(targetCategory.getName().getValue());
                        li.setPadding(new Insets(0, 1, 1, targetCategory.getDepth() * 10));
                        m.setContent(li);

                        m.setOnAction(event -> {
                            for (StoreEntryWrapper w : selection) {
                                w.moveTo(targetCategory.getCategory());
                            }
                            event.consume();
                        });
                        if (targetCategory.getParent() == null) {
                            m.setDisable(true);
                        }

                        menu.getItems().add(m);
                    });
            return menu;

        }));
        return button;
    }

    private BaseRegionBuilder<?, ?> buildDeleteButton() {
        var i18n = AppI18n.observable("delete");
        var name = Bindings.createStringBinding(() -> {
            return expanded.get() ? i18n.getValue() : null;
        }, expanded, i18n);
        var button = new ButtonComp(name, new SimpleObjectProperty<>(new LabelGraphic.IconGraphic("mdal-delete_outline")), () -> {
            var confirm = AppDialog.confirm("confirmDeletion");
            if (!confirm) {
                return;
            }

            var selection = StoreViewState.get().getBatchModeSelection().getList();
            for (StoreEntryWrapper w : selection) {
                w.delete();
            }
        });
        button.describe(d -> d.name(i18n));
        return button;
    }

    @SuppressWarnings("unchecked")
    private <T extends DataStore> MenuItem buildMenuItemForAction(List<DataStoreEntryRef<T>> batch, ActionProvider a) {
        BatchHubProvider<T> s = (BatchHubProvider<T>) a;
        var name = s.getName();
        var icon = s.getIcon();
        var children = s.getChildren(batch);
        if (children.size() > 0) {
            var menu = new Menu();
            menu.textProperty().bind(name);
            menu.setGraphic(icon.createGraphicNode());
            var items = children.stream()
                    .filter(actionProvider -> actionProvider instanceof BatchHubProvider<?>)
                    .map(c -> buildMenuItemForAction(batch, c))
                    .toList();
            menu.getItems().addAll(items);
            return menu;
        } else {
            var item = new MenuItem();
            item.textProperty().bind(name);
            item.setGraphic(icon.createGraphicNode());
            item.setOnAction(event -> {
                runActions(s);
                event.consume();
                if (event.getTarget() instanceof Menu m) {
                    m.getParentPopup().hide();
                }
            });
            return item;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends DataStore> void runActions(BatchHubProvider<?> s) {
        var l = new ArrayList<>(
                StoreViewState.get().getEffectiveBatchModeSelection().getList());
        var mapped = l.stream().map(w -> w.getEntry().<T>ref()).toList();
        ((BatchHubProvider<T>) s).execute(mapped);
    }
}
