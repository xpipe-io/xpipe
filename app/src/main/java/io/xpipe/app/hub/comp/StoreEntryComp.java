package io.xpipe.app.hub.comp;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.augment.ContextMenuAugment;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.*;
import io.xpipe.app.hub.action.HubBranchProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.HubMenuItemProvider;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;
import io.xpipe.core.process.OsType;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class StoreEntryComp extends SimpleComp {

    public static final PseudoClass FAILED = PseudoClass.getPseudoClass("failed");
    public static final PseudoClass INCOMPLETE = PseudoClass.getPseudoClass("incomplete");
    public static final ObservableDoubleValue INFO_NO_CONTENT_WIDTH = Bindings.createDoubleBinding(
            () -> {
                var w = App.getApp().getStage().getWidth();
                if (w >= 1000) {
                    return (w / 2.1) - 100;
                } else {
                    return (w / 1.7) - 50;
                }
            },
            App.getApp().getStage().widthProperty());
    public static final ObservableDoubleValue INFO_WITH_CONTENT_WIDTH = Bindings.createDoubleBinding(
            () -> {
                var w = App.getApp().getStage().getWidth();
                if (w >= 1000) {
                    return (w / 2.1) - 200;
                } else {
                    return (w / 1.7) - 150;
                }
            },
            App.getApp().getStage().widthProperty());
    protected final StoreSection section;
    protected final Comp<?> content;

    public StoreEntryComp(StoreSection section, Comp<?> content) {
        this.section = section;
        this.content = content;
    }

    public StoreEntryWrapper getWrapper() {
        return section.getWrapper();
    }

    public static StoreEntryComp create(StoreSection section, Comp<?> content, boolean preferLarge) {
        var forceCondensed = AppPrefs.get() != null
                && AppPrefs.get().condenseConnectionDisplay().get();
        if (!preferLarge || forceCondensed) {
            return new DenseStoreEntryComp(section, content);
        } else {
            return new StandardStoreEntryComp(section, content);
        }
    }

    public static StoreEntryComp customSection(StoreSection e) {
        var prov = e.getWrapper().getEntry().getProvider();
        if (prov != null) {
            return prov.customEntryComp(e, e.getDepth() == 1);
        } else {
            var forceCondensed = AppPrefs.get() != null
                    && AppPrefs.get().condenseConnectionDisplay().get();
            return forceCondensed ? new DenseStoreEntryComp(e, null) : new StandardStoreEntryComp(e, null);
        }
    }

    public abstract boolean isFullSize();

    public abstract int getHeight();

    @Override
    protected final Region createSimple() {
        var r = createContent();
        var name = (Region) r.lookup(".name");
        var buttonBar = r.lookup(".button-bar");
        var iconChooser = r.lookup(".icon");
        var batchMode = r.lookup(".batch-mode-selector");

        var button = new Button();
        button.setGraphic(r);
        GrowAugment.create(true, false).augment(new SimpleCompStructure<>(r));
        button.getStyleClass().add("store-entry-comp");
        button.setPadding(Insets.EMPTY);
        button.setMaxWidth(5000);
        button.setFocusTraversable(true);
        button.accessibleTextProperty().bind(getWrapper().getShownName());
        button.setOnAction(event -> {
            if (getWrapper().getRenaming().get()) {
                return;
            }

            event.consume();
            ThreadHelper.runFailableAsync(() -> {
                getWrapper().executeDefaultAction();
            });
        });
        button.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            var notOnButton = NodeHelper.isParent(iconChooser, event.getTarget())
                    || NodeHelper.isParent(batchMode, event.getTarget())
                    || NodeHelper.isParent(buttonBar, event.getTarget());
            if (AppPrefs.get().requireDoubleClickForConnections().get() && !notOnButton) {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() != 2) {
                    event.consume();
                }
            } else {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() > 1) {
                    event.consume();
                }
            }
        });
        button.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            var notOnButton = NodeHelper.isParent(iconChooser, event.getTarget())
                    || NodeHelper.isParent(batchMode, event.getTarget())
                    || NodeHelper.isParent(buttonBar, event.getTarget());
            if (AppPrefs.get().requireDoubleClickForConnections().get() && !notOnButton) {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() != 2) {
                    event.consume();
                }
            } else {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() > 1) {
                    event.consume();
                }
            }
        });
        new ContextMenuAugment<>(
                        mouseEvent -> mouseEvent.getButton() == MouseButton.SECONDARY,
                        null,
                        () -> this.createContextMenu(name))
                .augment(button);

        var loading = new LoadingOverlayComp(Comp.of(() -> button), getWrapper().getEffectiveBusy(), false);
        if (OsType.getLocal() == OsType.MACOS) {
            AppFontSizes.base(button);
        } else if (OsType.getLocal() == OsType.LINUX) {
            AppFontSizes.xl(button);
        } else {
            AppFontSizes.apply(button, sizes -> {
                if (sizes.getBase().equals("10.5")) {
                    return sizes.getXl();
                } else {
                    return sizes.getLg();
                }
            });
        }
        return loading.createRegion();
    }

    protected abstract Region createContent();

    protected void applyState(Node node) {
        getWrapper().getValidity().subscribe(val -> {
            switch (val) {
                case LOAD_FAILED -> {
                    node.pseudoClassStateChanged(FAILED, true);
                    node.pseudoClassStateChanged(INCOMPLETE, false);
                }
                case INCOMPLETE -> {
                    node.pseudoClassStateChanged(FAILED, false);
                    node.pseudoClassStateChanged(INCOMPLETE, true);
                }
                default -> {
                    node.pseudoClassStateChanged(FAILED, false);
                    node.pseudoClassStateChanged(INCOMPLETE, false);
                }
            }
        });
    }

    protected Comp<?> createName() {
        var prop = new SimpleStringProperty();
        getWrapper().getShownName().subscribe(prop::setValue);
        prop.addListener((observable, oldValue, newValue) -> {
            if (!AppPrefs.get().censorMode().get()) {
                getWrapper().getName().setValue(newValue);
            }
        });
        var name = new LazyTextFieldComp(prop);
        name.styleClass("name");
        name.apply(struc -> {
            getWrapper().getRenaming().bind(struc.getTextField().focusedProperty());
        });
        return name;
    }

    protected Comp<?> createOrderIndex() {
        var prop = new SimpleStringProperty();
        getWrapper().getOrderIndex().subscribe(number -> {
            var i = number.intValue();
            var displayed = i == Integer.MIN_VALUE ? "-" : i == Integer.MAX_VALUE ? "+" : i != 0 ? "" + i : null;
            prop.set(displayed != null ? "[" + displayed + "]" : null);
        });
        var comp = new LabelComp(prop);
        comp.styleClass("orderIndex");
        return comp;
    }

    protected Comp<?> createUserIcon() {
        var button = new IconButtonComp("mdi2a-account");
        button.styleClass("user-icon");
        button.tooltipKey("personalConnection");
        button.apply(struc -> {
            AppFontSizes.base(struc.get());
            struc.get().setOpacity(1.0);
        });
        button.hide(Bindings.not(getWrapper().getPerUser()));
        return button;
    }

    protected Comp<?> createPinIcon() {
        var button = new IconButtonComp("mdi2p-pin-outline");
        button.disable(new SimpleBooleanProperty(true));
        button.tooltipKey("pinned");
        button.apply(struc -> {
            AppFontSizes.xs(struc.get());
            struc.get().setOpacity(1.0);
        });
        button.hide(Bindings.not(getWrapper().getPinToTop()));
        return button;
    }

    protected Node createIcon(int w, int h, Consumer<Node> fontSize) {
        var icon = new StoreIconComp(getWrapper(), w, h);
        icon.apply(struc -> {
            struc.get()
                    .opacityProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                if (!getWrapper().getValidity().getValue().isUsable()) {
                                    return 0.5;
                                }

                                return !getWrapper().getEffectiveBusy().get() ? 1.0 : 0.15;
                            },
                            getWrapper().getValidity(),
                            getWrapper().getEffectiveBusy()));
        });
        var loading = new LoadingIconComp(getWrapper().getEffectiveBusy(), fontSize);
        loading.prefWidth(w);
        loading.prefHeight(h);
        var stack = new StackComp(List.of(icon, loading));
        return stack.createRegion();
    }

    protected Region createButtonBar(Region name) {
        var list = DerivedObservableList.wrap(getWrapper().getMajorActionProviders(), false);
        var buttons = list.mapped(actionProvider -> {
                    var button = buildButton(actionProvider);
                    return button.createRegion();
                })
                .filtered(region -> region != null)
                .getList();

        var ig = new InputGroup();
        Runnable update = () -> {
            var l = new ArrayList<Node>(buttons);
            var settingsButton = createSettingsButton(name).createRegion();
            l.add(settingsButton);
            l.forEach(o -> o.getStyleClass().remove(Styles.FLAT));
            ig.getChildren().setAll(l);
        };
        buttons.subscribe(update);
        update.run();
        ig.setAlignment(Pos.CENTER_RIGHT);
        ig.getStyleClass().add("button-bar");
        AppFontSizes.base(ig);
        return ig;
    }

    private Comp<?> buildButton(HubMenuItemProvider<?> p) {
        var leaf = p instanceof HubLeafProvider<?> l ? l : null;
        var branch = p instanceof HubBranchProvider<?> b ? b : null;
        var button = new IconButtonComp(
                p.getIcon(getWrapper().getEntry().ref()),
                leaf != null
                        ? () -> {
                            leaf.execute(getWrapper().getEntry().ref());
                        }
                        : null);
        if (branch != null) {
            button.apply(new ContextMenuAugment<>(
                    mouseEvent -> mouseEvent.getButton() == MouseButton.PRIMARY, keyEvent -> false, () -> {
                        var cm = ContextMenuHelper.create();
                        var children =
                                branch.getChildren(getWrapper().getEntry().ref());
                        var cats = Arrays.stream(StoreActionCategory.values())
                                .collect(Collectors.toCollection(ArrayList::new));
                        cats.addFirst(null);
                        for (var cat : cats) {
                            var catChildren = children.stream()
                                    .filter(actionProvider -> actionProvider.getCategory() == cat)
                                    .toList();
                            if (catChildren.isEmpty()) {
                                continue;
                            }

                            catChildren.forEach(childProvider -> {
                                var menu = buildMenuItemForAction(childProvider);
                                if (menu != null) {
                                    cm.getItems().add(menu);
                                }
                            });
                            cm.getItems().add(new SeparatorMenuItem());
                        }

                        if (cm.getItems().getLast() instanceof SeparatorMenuItem) {
                            cm.getItems().removeLast();
                        }

                        return cm;
                    }));
        }
        button.accessibleText(p.getName(getWrapper().getEntry().ref()).getValue());
        button.tooltip(p.getName(getWrapper().getEntry().ref()));
        return button;
    }

    protected Comp<?> createSettingsButton(Region name) {
        var settingsButton = new IconButtonComp("mdi2d-dots-horizontal-circle-outline", null);
        settingsButton.styleClass("settings");
        settingsButton.accessibleText("More");
        settingsButton.apply(new ContextMenuAugment<>(
                event -> event.getButton() == MouseButton.PRIMARY,
                null,
                () -> StoreEntryComp.this.createContextMenu(name)));
        settingsButton.tooltipKey("more");
        return settingsButton;
    }

    protected Comp<?> createBatchSelection() {
        var c = new StoreEntryBatchSelectComp(section);
        c.hide(StoreViewState.get().getBatchMode().not());
        return c;
    }

    protected ContextMenu createContextMenu(Region name) {
        var contextMenu = ContextMenuHelper.create();

        var cats = Arrays.stream(StoreActionCategory.values()).collect(Collectors.toCollection(ArrayList::new));
        cats.addFirst(null);
        for (var cat : cats) {
            var items = new ArrayList<MenuItem>();

            for (var p : getWrapper().getMinorActionProviders()) {
                var item = buildMenuItemForAction(p);
                if (item == null || p.getCategory() != cat) {
                    continue;
                }

                items.add(item);
            }

            if (cat == StoreActionCategory.CONFIGURATION
                    && getWrapper().getEntry().getValidity() != DataStoreEntry.Validity.LOAD_FAILED) {
                var rename = new MenuItem(AppI18n.get("rename"), new FontIcon("mdal-edit"));
                rename.setOnAction(event -> {
                    name.requestFocus();
                });
                items.add(1, rename);

                var notes = new MenuItem(AppI18n.get("addNotes"), new FontIcon("mdi2c-comment-text-outline"));
                notes.setOnAction(event -> {
                    getWrapper().getNotes().setValue(new StoreNotes(null, getDefaultNotes()));
                    event.consume();
                });
                notes.visibleProperty().bind(BindingsHelper.map(getWrapper().getNotes(), s -> s.getCommited() == null));
                items.add(2, notes);

                var freeze = new MenuItem();
                freeze.graphicProperty()
                        .bind(Bindings.createObjectBinding(
                                () -> {
                                    var is = getWrapper().getReadOnly().get();
                                    return is
                                            ? new FontIcon("mdi2l-lock-open-variant-outline")
                                            : new FontIcon("mdi2l-lock-open-outline");
                                },
                                getWrapper().getReadOnly()));
                freeze.textProperty()
                        .bind(Bindings.createStringBinding(
                                () -> {
                                    var is = getWrapper().getReadOnly().get();
                                    return is
                                            ? AppI18n.get("unfreezeConfiguration")
                                            : AppI18n.get("freezeConfiguration");
                                },
                                AppI18n.activeLanguage(),
                                getWrapper().getReadOnly()));
                freeze.setOnAction(event -> getWrapper()
                        .getEntry()
                        .setFreeze(!getWrapper().getReadOnly().get()));
                items.add(freeze);
            }

            if (cat == StoreActionCategory.DEVELOPER) {
                if (AppPrefs.get().developerMode().getValue()) {
                    var browse = new MenuItem(
                            AppI18n.get("browseInternalStorage"), new FontIcon("mdi2f-folder-open-outline"));
                    browse.setOnAction(event -> DesktopHelper.browsePathLocal(
                            getWrapper().getEntry().getDirectory()));
                    items.add(browse);
                }

                if (AppPrefs.get().enableHttpApi().get()) {
                    var copyId = new MenuItem(AppI18n.get("copyId"), new FontIcon("mdi2c-content-copy"));
                    copyId.setOnAction(event -> ClipboardHelper.copyText(
                            getWrapper().getEntry().getUuid().toString()));
                    items.add(copyId);
                }
            }

            if (cat == StoreActionCategory.APPEARANCE) {
                if (section.getDepth() == 1) {
                    var color = new Menu(AppI18n.get("color"), new FontIcon("mdi2f-format-color-fill"));
                    var none = new MenuItem();
                    none.textProperty().bind(AppI18n.observable("none"));
                    none.setOnAction(event -> {
                        getWrapper().getEntry().setColor(null);
                        event.consume();
                    });
                    color.getItems().add(none);
                    Arrays.stream(DataStoreColor.values()).forEach(dataStoreColor -> {
                        MenuItem m = new MenuItem();
                        m.textProperty().bind(AppI18n.observable(dataStoreColor.getId()));
                        m.setOnAction(event -> {
                            getWrapper().getEntry().setColor(dataStoreColor);
                            event.consume();
                        });
                        color.getItems().add(m);
                    });
                    items.add(color);
                }

                {
                    var order = new Menu(AppI18n.get("order"), new FontIcon("mdi2f-format-list-bulleted"));

                    var index = new MenuItem(AppI18n.get("index"), new FontIcon("mdi2o-order-numeric-ascending"));
                    index.setOnAction(event -> {
                        StoreOrderIndexDialog.show(getWrapper());
                        event.consume();
                    });
                    order.getItems().add(index);

                    order.getItems().add(new SeparatorMenuItem());

                    var noOrder = new MenuItem(AppI18n.get("none"), new FontIcon("mdi2r-reorder-horizontal"));
                    noOrder.setOnAction(event -> {
                        DataStorage.get().setOrderIndex(getWrapper().getEntry(), 0);
                        event.consume();
                    });
                    if (getWrapper().getEntry().getOrderIndex() == Integer.MIN_VALUE
                            && getWrapper().getEntry().getOrderIndex() == Integer.MAX_VALUE) {
                        order.getItems().add(noOrder);
                    }

                    var first = new MenuItem(AppI18n.get("moveToFirst"), new FontIcon("mdi2o-order-bool-descending"));
                    first.setOnAction(event -> {
                        getWrapper().orderFirst();
                        event.consume();
                    });
                    order.getItems().add(first);

                    var last = new MenuItem(AppI18n.get("moveToLast"), new FontIcon("mdi2o-order-bool-ascending"));
                    last.setOnAction(event -> {
                        getWrapper().orderLast();
                        event.consume();
                    });
                    order.getItems().add(last);

                    order.getItems().add(new SeparatorMenuItem());

                    var top =
                            new MenuItem(AppI18n.get("keepFirst"), new FontIcon("mdi2o-order-bool-descending-variant"));
                    top.setOnAction(event -> {
                        getWrapper().orderStickFirst();
                        event.consume();
                    });
                    top.setDisable(getWrapper().getEntry().getOrderIndex() == Integer.MIN_VALUE);
                    order.getItems().add(top);

                    var bottom =
                            new MenuItem(AppI18n.get("keepLast"), new FontIcon("mdi2o-order-bool-ascending-variant"));
                    bottom.setOnAction(event -> {
                        getWrapper().orderStickLast();
                        event.consume();
                    });
                    bottom.setDisable(getWrapper().getEntry().getOrderIndex() == Integer.MAX_VALUE);
                    order.getItems().add(bottom);

                    order.getItems().add(new SeparatorMenuItem());

                    items.add(order);
                }

                if (getWrapper().getEntry().getProvider() != null
                        && getWrapper().getEntry().getProvider().canMoveCategories()) {
                    var move = new Menu(AppI18n.get("category"), new FontIcon("mdi2f-folder-move-outline"));
                    StoreViewState.get()
                            .getSortedCategories(
                                    getWrapper().getCategory().getValue().getRoot())
                            .getList()
                            .forEach(storeCategoryWrapper -> {
                                MenuItem m = new MenuItem();
                                m.textProperty()
                                        .setValue("  ".repeat(storeCategoryWrapper.getDepth())
                                                + storeCategoryWrapper.getName().getValue());
                                m.setOnAction(event -> {
                                    getWrapper().moveTo(storeCategoryWrapper.getCategory());
                                    event.consume();
                                });
                                if (storeCategoryWrapper.getParent() == null) {
                                    m.setDisable(true);
                                }

                                move.getItems().add(m);
                            });
                    items.add(move);
                }

                if (getWrapper().getPinToTop().getValue() || section.getDepth() > 1) {
                    var pinToTop = new MenuItem();
                    pinToTop.graphicProperty()
                            .bind(Bindings.createObjectBinding(
                                    () -> {
                                        var is = getWrapper().getPinToTop().get();
                                        return is
                                                ? new FontIcon("mdi2p-pin-off-outline")
                                                : new FontIcon("mdi2p-pin-outline");
                                    },
                                    getWrapper().getPinToTop()));
                    pinToTop.textProperty()
                            .bind(Bindings.createStringBinding(
                                    () -> {
                                        var is = getWrapper().getPinToTop().get();
                                        return is ? AppI18n.get("unpinFromTop") : AppI18n.get("pinToTop");
                                    },
                                    AppI18n.activeLanguage(),
                                    getWrapper().getPinToTop()));
                    pinToTop.setOnAction(event -> getWrapper().togglePinToTop());
                    items.add(pinToTop);
                }

                if (getWrapper().getStore().getValue() instanceof FixedHierarchyStore) {
                    var breakOut = new MenuItem();
                    var is = getWrapper().getEntry().getBreakOutCategory() != null;
                    if (is) {
                        breakOut.textProperty().bind(AppI18n.observable("mergeCategory"));
                        breakOut.setGraphic(new FontIcon("mdi2c-collapse-all-outline"));
                    } else {
                        breakOut.textProperty().bind(AppI18n.observable("breakOutCategory"));
                        breakOut.setGraphic(new FontIcon("mdi2e-expand-all-outline"));
                    }
                    breakOut.setOnAction(event -> {
                        if (is) {
                            getWrapper().mergeBreakOutCategory();
                        } else {
                            getWrapper().breakOutCategory();
                        }
                        event.consume();
                    });
                    items.add(breakOut);
                }
            }

            if (cat == StoreActionCategory.DELETION) {
                var del = new MenuItem(AppI18n.get("remove"), new FontIcon("mdal-delete_outline"));
                del.disableProperty()
                        .bind(Bindings.createBooleanBinding(
                                () -> {
                                    return !getWrapper().getDeletable().get();
                                },
                                getWrapper().getDeletable()));
                del.setOnAction(event -> getWrapper().delete());
                contextMenu.getItems().add(del);
            }

            if (items.isEmpty()) {
                continue;
            }

            contextMenu.getItems().addAll(items);
            contextMenu.getItems().add(new SeparatorMenuItem());
        }

        return contextMenu;
    }

    private MenuItem buildMenuItemForAction(ActionProvider p) {
        var leaf = p instanceof HubLeafProvider<?> l ? l : null;
        var branch = p instanceof HubBranchProvider<?> b ? b : null;
        var cs = leaf != null ? leaf : branch;

        if (cs == null
                || cs.isMajor(getWrapper().getEntry().ref())
                || (leaf != null && leaf.isDefault(getWrapper().getEntry().ref()))) {
            return null;
        }

        var name = cs.getName(getWrapper().getEntry().ref());
        var icon = cs.getIcon(getWrapper().getEntry().ref());
        var item = branch != null
                ? new Menu(null, icon.createGraphicNode())
                : new MenuItem(null, icon.createGraphicNode());

        var proRequired = p.getLicensedFeatureId() != null
                && !LicenseProvider.get().getFeature(p.getLicensedFeatureId()).isSupported();
        if (proRequired) {
            item.setDisable(true);
            item.textProperty()
                    .bind(LicenseProvider.get()
                            .getFeature(p.getLicensedFeatureId())
                            .suffixObservable(name.getValue()));
        } else {
            item.textProperty().bind(name);
        }
        Menu menu = item instanceof Menu m ? m : null;

        if (branch != null) {
            var items = branch.getChildren(getWrapper().getEntry().ref()).stream()
                    .filter(actionProvider -> getWrapper().showActionProvider(actionProvider, false))
                    .map(c -> buildMenuItemForAction(c))
                    .toList();
            menu.getItems().addAll(items);
            return menu;
        }

        item.setOnAction(event -> {
            leaf.execute(getWrapper().getEntry().ref());
            event.consume();
            if (event.getTarget() instanceof Menu m) {
                m.getParentPopup().hide();
            }
        });

        return item;
    }

    private static String DEFAULT_NOTES = null;

    private static String getDefaultNotes() {
        if (DEFAULT_NOTES == null) {
            AppResources.with(AppResources.XPIPE_MODULE, "misc/notes_default.md", f -> {
                DEFAULT_NOTES = Files.readString(f);
            });
        }
        return DEFAULT_NOTES;
    }
}
