package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.augment.ContextMenuAugment;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.comp.base.TooltipAugment;
import io.xpipe.app.core.*;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.resources.AppResources;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.storage.DataStorage;
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

public abstract class StoreEntryComp extends SimpleComp {

    public static final PseudoClass FAILED = PseudoClass.getPseudoClass("failed");
    public static final PseudoClass INCOMPLETE = PseudoClass.getPseudoClass("incomplete");
    public static final ObservableDoubleValue INFO_NO_CONTENT_WIDTH =
            App.getApp().getStage().widthProperty().divide(2.1).add(-100);
    public static final ObservableDoubleValue INFO_WITH_CONTENT_WIDTH =
            App.getApp().getStage().widthProperty().divide(2.1).add(-200);
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

        var loading = LoadingOverlayComp.noProgress(
                Comp.of(() -> button), getWrapper().getEffectiveBusy());
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
        PlatformThread.sync(getWrapper().getValidity()).subscribe(val -> {
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

    protected Node createIcon(int w, int h) {
        return new StoreIconComp(getWrapper(), w, h).createRegion();
    }

    protected Region createButtonBar() {
        var ig = new InputGroup();
        Runnable update = () -> {
            var buttons = new ArrayList<Node>();
            for (ActionProvider actionProvider : getWrapper().getActionProviders()) {
                var button = buildButton(actionProvider);
                if (button != null) {
                    buttons.add(button.createRegion());
                }
            }
            var l = new ArrayList<>(buttons);
            var settingsButton = createSettingsButton().createRegion();
            l.add(settingsButton);
            l.forEach(o -> o.getStyleClass().remove(Styles.FLAT));
            ig.getChildren().setAll(l);
        };
        update.run();
        getWrapper().getActionProviders().addListener((observableValue, actionProviders, t1) -> {
            update.run();
        });
        update.run();
        ig.setAlignment(Pos.CENTER_RIGHT);
        ig.getStyleClass().add("button-bar");
        AppFontSizes.base(ig);
        return ig;
    }

    private Comp<?> buildButton(ActionProvider p) {
        var leaf = p.getLeafDataStoreCallSite();
        var branch = p.getBranchDataStoreCallSite();
        var cs = leaf != null ? leaf : branch;

        if (cs == null || !cs.isMajor(getWrapper().getEntry().ref())) {
            return null;
        }

        var button = new IconButtonComp(
                cs.getIcon(getWrapper().getEntry().ref()),
                leaf != null
                        ? () -> {
                            ThreadHelper.runFailableAsync(() -> {
                                getWrapper()
                                        .runAction(
                                                leaf.createAction(
                                                        getWrapper().getEntry().ref()),
                                                leaf.showBusy());
                            });
                        }
                        : null);
        if (branch != null) {
            button.apply(new ContextMenuAugment<>(
                    mouseEvent -> mouseEvent.getButton() == MouseButton.PRIMARY, keyEvent -> false, () -> {
                        var cm = ContextMenuHelper.create();
                        branch.getChildren(getWrapper().getEntry().ref()).stream()
                                .filter(actionProvider -> getWrapper().showActionProvider(actionProvider))
                                .forEach(childProvider -> {
                                    var menu = buildMenuItemForAction(childProvider);
                                    if (menu != null) {
                                        cm.getItems().add(menu);
                                    }
                                });
                        return cm;
                    }));
        }
        button.accessibleText(cs.getName(getWrapper().getEntry().ref()).getValue());
        button.apply(new TooltipAugment<>(cs.getName(getWrapper().getEntry().ref()), null));
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

    protected ContextMenu createContextMenu() {
        var contextMenu = ContextMenuHelper.create();

        var hasSep = false;
        for (var p : getWrapper().getActionProviders()) {
            var item = buildMenuItemForAction(p);
            if (item == null) {
                continue;
            }

            if (p.getLeafDataStoreCallSite() != null
                    && p.getLeafDataStoreCallSite().isSystemAction()
                    && !hasSep) {
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
            Arrays.stream(DataColor.values()).forEach(dataStoreColor -> {
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
        var leaf = p.getLeafDataStoreCallSite();
        var branch = p.getBranchDataStoreCallSite();
        var cs = leaf != null ? leaf : branch;

        if (cs == null || cs.isMajor(getWrapper().getEntry().ref())) {
            return null;
        }

        var name = cs.getName(getWrapper().getEntry().ref());
        var icon = cs.getIcon(getWrapper().getEntry().ref());
        var item = (leaf != null && leaf.canLinkTo()) || branch != null
                ? new Menu(null, new FontIcon(icon))
                : new MenuItem(null, new FontIcon(icon));

        var proRequired = p.getProFeatureId() != null
                && !LicenseProvider.get().getFeature(p.getProFeatureId()).isSupported();
        if (proRequired) {
            item.setDisable(true);
            item.textProperty()
                    .bind(LicenseProvider.get().getFeature(p.getProFeatureId()).suffixObservable(name.getValue()));
        } else {
            item.textProperty().bind(name);
        }
        Menu menu = item instanceof Menu m ? m : null;

        if (branch != null) {
            var items = branch.getChildren(getWrapper().getEntry().ref()).stream()
                    .filter(actionProvider -> getWrapper().showActionProvider(actionProvider))
                    .map(c -> buildMenuItemForAction(c))
                    .toList();
            menu.getItems().addAll(items);
            return menu;
        } else if (leaf.canLinkTo()) {
            var run = new MenuItem(null, new FontIcon("mdi2c-code-greater-than"));
            run.textProperty().bind(AppI18n.observable("base.execute"));
            run.setOnAction(event -> {
                ThreadHelper.runFailableAsync(() -> {
                    getWrapper()
                            .runAction(leaf.createAction(getWrapper().getEntry().ref()), leaf.showBusy());
                });
                event.consume();
            });
            menu.getItems().add(run);

            var sc = new MenuItem(null, new FontIcon("mdi2c-code-greater-than"));
            var url = "xpipe://action/" + p.getId() + "/"
                    + getWrapper().getEntry().getUuid();
            sc.textProperty().bind(AppI18n.observable("base.createShortcut"));
            sc.setOnAction(event -> {
                ThreadHelper.runFailableAsync(() -> {
                    DesktopShortcuts.createCliOpen(
                            url,
                            DataStorage.get()
                                            .getStoreEntryDisplayName(
                                                    getWrapper().getEntry()) + " ("
                                    + p.getLeafDataStoreCallSite()
                                            .getName(getWrapper().getEntry().ref())
                                            .getValue() + ")");
                });
                event.consume();
            });
            menu.getItems().add(sc);

            if (AppDistributionType.get().isSupportsUrls()) {
                var l = new MenuItem(null, new FontIcon("mdi2c-clipboard-list-outline"));
                l.textProperty().bind(AppI18n.observable("base.copyShareLink"));
                l.setOnAction(event -> {
                    ThreadHelper.runFailableAsync(() -> {
                        AppActionLinkDetector.setLastDetectedAction(url);
                        ClipboardHelper.copyUrl(url);
                    });
                    event.consume();
                });
                menu.getItems().add(l);
            }
        }

        item.setOnAction(event -> {
            if (menu != null && !event.getTarget().equals(menu)) {
                return;
            }

            if (menu != null && menu.isDisable()) {
                return;
            }

            ThreadHelper.runFailableAsync(() -> {
                getWrapper().runAction(leaf.createAction(getWrapper().getEntry().ref()), leaf.showBusy());
            });
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
