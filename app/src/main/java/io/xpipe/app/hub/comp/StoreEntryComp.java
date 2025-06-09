package io.xpipe.app.hub.comp;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.action.BranchStoreActionProvider;
import io.xpipe.app.action.LeafStoreActionProvider;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.augment.ContextMenuAugment;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.*;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.hub.action.StoreActionProvider;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;
import io.xpipe.core.process.OsType;

import javafx.beans.binding.Bindings;
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
                        () -> this.createContextMenu())
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
        LabelComp name = new LabelComp(getWrapper().getShownName());
        name.apply(struc -> struc.get().setTextOverrun(OverrunStyle.CENTER_ELLIPSIS));
        name.styleClass("name");
        return name;
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

    protected Node createIcon(int w, int h, Consumer<Node> fontSize) {
        var icon = new StoreIconComp(getWrapper(), w, h);
        icon.apply(struc -> {
            struc.get()
                    .opacityProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                return !getWrapper().getEffectiveBusy().get() ? 1.0 : 0.15;
                            },
                            getWrapper().getEffectiveBusy()));
        });
        var loading = new LoadingIconComp(getWrapper().getEffectiveBusy(), fontSize);
        loading.prefWidth(w);
        loading.prefHeight(h);
        var stack = new StackComp(List.of(icon, loading));
        return stack.createRegion();
    }

    protected Region createButtonBar() {
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
            var settingsButton = createSettingsButton().createRegion();
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

    private Comp<?> buildButton(StoreActionProvider<?> p) {
        var leaf = p instanceof LeafStoreActionProvider<?> l ? l : null;
        var branch = p instanceof BranchStoreActionProvider<?> b ? b : null;
        var button = new IconButtonComp(
                p.getIcon(getWrapper().getEntry().ref()),
                leaf != null
                        ? () -> {
                            leaf.createAction(getWrapper().getEntry().ref()).executeAsync();
                        }
                        : null);
        if (branch != null) {
            button.apply(new ContextMenuAugment<>(
                    mouseEvent -> mouseEvent.getButton() == MouseButton.PRIMARY, keyEvent -> false, () -> {
                        var cm = ContextMenuHelper.create();
                        branch.getChildren(getWrapper().getEntry().ref()).stream()
                                .filter(actionProvider -> getWrapper().showActionProvider(actionProvider, false))
                                .forEach(childProvider -> {
                                    var menu = buildMenuItemForAction(childProvider);
                                    if (menu != null) {
                                        cm.getItems().add(menu);
                                    }
                                });
                        return cm;
                    }));
        }
        button.accessibleText(p.getName(getWrapper().getEntry().ref()).getValue());
        button.tooltip(p.getName(getWrapper().getEntry().ref()));
        return button;
    }

    protected Comp<?> createSettingsButton() {
        var settingsButton = new IconButtonComp("mdi2d-dots-horizontal-circle-outline", null);
        settingsButton.styleClass("settings");
        settingsButton.accessibleText("More");
        settingsButton.apply(new ContextMenuAugment<>(
                event -> event.getButton() == MouseButton.PRIMARY,
                null,
                () -> StoreEntryComp.this.createContextMenu()));
        settingsButton.tooltipKey("more");
        return settingsButton;
    }

    protected Comp<?> createBatchSelection() {
        var c = new StoreEntryBatchSelectComp(section);
        c.hide(StoreViewState.get().getBatchMode().not());
        return c;
    }

    protected ContextMenu createContextMenu() {
        var contextMenu = ContextMenuHelper.create();

        var hasSep = false;
        for (var p : getWrapper().getMinorActionProviders()) {
            var item = buildMenuItemForAction(p);
            if (item == null) {
                continue;
            }

            if (p instanceof LeafStoreActionProvider<?> l && l.isSystemAction() && !hasSep) {
                if (contextMenu.getItems().size() > 0) {
                    contextMenu.getItems().add(new SeparatorMenuItem());
                }
                hasSep = true;
            }

            contextMenu.getItems().add(item);
        }
        if (contextMenu.getItems().size() > 0 && !hasSep) {
            contextMenu.getItems().add(new SeparatorMenuItem());
        }

        var notes = new MenuItem(AppI18n.get("addNotes"), new FontIcon("mdi2n-note-text"));
        notes.setOnAction(event -> {
            getWrapper().getNotes().setValue(new StoreNotes(null, getDefaultNotes()));
            event.consume();
        });
        notes.visibleProperty().bind(BindingsHelper.map(getWrapper().getNotes(), s -> s.getCommited() == null));
        contextMenu.getItems().add(notes);

        if (AppPrefs.get().developerMode().getValue()) {
            var browse = new MenuItem(AppI18n.get("browseInternalStorage"), new FontIcon("mdi2f-folder-open-outline"));
            browse.setOnAction(event ->
                    DesktopHelper.browsePathLocal(getWrapper().getEntry().getDirectory()));
            contextMenu.getItems().add(browse);
        }

        if (AppPrefs.get().enableHttpApi().get()) {
            var copyId = new MenuItem(AppI18n.get("copyId"), new FontIcon("mdi2c-content-copy"));
            copyId.setOnAction(event ->
                    ClipboardHelper.copyText(getWrapper().getEntry().getUuid().toString()));
            contextMenu.getItems().add(copyId);
        }

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
            contextMenu.getItems().add(color);
        }

        if (getWrapper().getEntry().getProvider() != null
                && getWrapper().getEntry().getProvider().canMoveCategories()) {
            var move = new Menu(AppI18n.get("moveTo"), new FontIcon("mdi2f-folder-move-outline"));
            StoreViewState.get()
                    .getSortedCategories(getWrapper().getCategory().getValue().getRoot())
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
            contextMenu.getItems().add(move);
        }
        {
            var order = new Menu(AppI18n.get("order"), new FontIcon("mdal-bookmarks"));
            var noOrder = new MenuItem(AppI18n.get("none"), new FontIcon("mdi2r-reorder-horizontal"));
            noOrder.setOnAction(event -> {
                getWrapper().setOrder(null);
                event.consume();
            });
            if (getWrapper().getEntry().getExplicitOrder() == null) {
                noOrder.setDisable(true);
            }
            order.getItems().add(noOrder);
            order.getItems().add(new SeparatorMenuItem());

            var top = new MenuItem(AppI18n.get("stickToTop"), new FontIcon("mdi2o-order-bool-descending"));
            top.setOnAction(event -> {
                getWrapper().setOrder(DataStoreEntry.Order.TOP);
                event.consume();
            });
            if (DataStoreEntry.Order.TOP.equals(getWrapper().getEntry().getExplicitOrder())) {
                top.setDisable(true);
            }
            order.getItems().add(top);

            var bottom = new MenuItem(AppI18n.get("stickToBottom"), new FontIcon("mdi2o-order-bool-ascending"));
            bottom.setOnAction(event -> {
                getWrapper().setOrder(DataStoreEntry.Order.BOTTOM);
                event.consume();
            });
            if (DataStoreEntry.Order.BOTTOM.equals(getWrapper().getEntry().getExplicitOrder())) {
                bottom.setDisable(true);
            }
            order.getItems().add(bottom);
            contextMenu.getItems().add(order);
        }

        var readOnly = new MenuItem();
        readOnly.graphicProperty()
                .bind(Bindings.createObjectBinding(
                        () -> {
                            var is = getWrapper().getReadOnly().get();
                            return is
                                    ? new FontIcon("mdi2l-lock-open-variant-outline")
                                    : new FontIcon("mdi2l-lock-open-outline");
                        },
                        getWrapper().getReadOnly()));
        readOnly.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> {
                            var is = getWrapper().getReadOnly().get();
                            return is ? AppI18n.get("unsetReadOnly") : AppI18n.get("setReadOnly");
                        },
                        AppI18n.activeLanguage(),
                        getWrapper().getReadOnly()));
        readOnly.setOnAction(event ->
                getWrapper().getEntry().setReadOnly(!getWrapper().getReadOnly().get()));
        contextMenu.getItems().add(readOnly);

        contextMenu.getItems().add(new SeparatorMenuItem());

        var del = new MenuItem(AppI18n.get("remove"), new FontIcon("mdal-delete_outline"));
        del.disableProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> {
                            return !getWrapper().getDeletable().get();
                        },
                        getWrapper().getDeletable()));
        del.setOnAction(event -> getWrapper().delete());
        contextMenu.getItems().add(del);

        return contextMenu;
    }

    private MenuItem buildMenuItemForAction(ActionProvider p) {
        var leaf = p instanceof LeafStoreActionProvider<?> l ? l : null;
        var branch = p instanceof BranchStoreActionProvider<?> b ? b : null;
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
            leaf.createAction(getWrapper().getEntry().ref()).executeAsync();
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
