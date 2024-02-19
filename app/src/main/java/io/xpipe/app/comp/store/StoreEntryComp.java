package io.xpipe.app.comp.store;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.core.App;
import io.xpipe.app.core.AppActionLinkDetector;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.impl.*;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class StoreEntryComp extends SimpleComp {

    public static StoreEntryComp create(
            StoreEntryWrapper entry, Comp<?> content, boolean preferLarge) {
        var forceCondensed = AppPrefs.get() != null && AppPrefs.get().condenseConnectionDisplay().get();
        if (!preferLarge || forceCondensed) {
            return new DenseStoreEntryComp(entry, true, content);
        } else {
            return new StandardStoreEntryComp(entry, content);
        }
    }

    public static Comp<?> customSection(StoreSection e, boolean topLevel) {
        var prov = e.getWrapper().getEntry().getProvider();
        if (prov != null) {
            return prov.customEntryComp(e, topLevel);
        } else {
            var forceCondensed = AppPrefs.get() != null && AppPrefs.get().condenseConnectionDisplay().get();
            return forceCondensed ?
                    new DenseStoreEntryComp(e.getWrapper(), true, null) :
                    new StandardStoreEntryComp(e.getWrapper(), null);
        }
    }

    public static final PseudoClass FAILED = PseudoClass.getPseudoClass("failed");
    public static final PseudoClass INCOMPLETE = PseudoClass.getPseudoClass("incomplete");
    public static final ObservableDoubleValue INFO_NO_CONTENT_WIDTH =
            App.getApp().getStage().widthProperty().divide(2.2).add(-100);
    public static final ObservableDoubleValue INFO_WITH_CONTENT_WIDTH =
            App.getApp().getStage().widthProperty().divide(2.2).add(-200);
    protected final StoreEntryWrapper wrapper;
    protected final Comp<?> content;

    public StoreEntryComp(StoreEntryWrapper wrapper, Comp<?> content) {
        this.wrapper = wrapper;
        this.content = content;
    }

    @Override
    protected final Region createSimple() {
        var r = createContent();

        var button = new Button();
        button.setGraphic(r);
        GrowAugment.create(true, false).augment(new SimpleCompStructure<>(r));
        button.getStyleClass().add("store-entry-comp");
        button.setPadding(Insets.EMPTY);
        button.setMaxWidth(5000);
        button.setFocusTraversable(true);
        button.accessibleTextProperty()
                .bind(wrapper.nameProperty());
        button.setOnAction(event -> {
            event.consume();
            ThreadHelper.runFailableAsync(() -> {
                wrapper.executeDefaultAction();
            });
        });
        new ContextMenuAugment<>(() -> this.createContextMenu()).augment(new SimpleCompStructure<>(button));

        var loading = LoadingOverlayComp.noProgress(
                Comp.of(() -> button),
                BindingsHelper.persist(
                        wrapper.getInRefresh().and(wrapper.getObserving().not())));
        return loading.createRegion();
    }

    protected abstract Region createContent();

    protected Label createInformation() {
        var information = new Label();
        information.setGraphicTextGap(7);
        information.textProperty().bind(wrapper.getEntry().getProvider() != null ?
                                                PlatformThread.sync(wrapper.getEntry().getProvider().informationString(wrapper)) : new SimpleStringProperty());
        information.getStyleClass().add("information");
        AppFont.header(information);

        var state = wrapper.getEntry().getProvider() != null
                ? wrapper.getEntry().getProvider().stateDisplay(wrapper)
                : Comp.empty();
        information.setGraphic(state.createRegion());

        return information;
    }

    protected Label createSummary() {
        var summary = new Label();
        summary.textProperty().bind(wrapper.getSummary());
        summary.getStyleClass().add("summary");
        AppFont.small(summary);
        return summary;
    }

    protected void applyState(Node node) {
        SimpleChangeListener.apply(PlatformThread.sync(wrapper.getValidity()), val -> {
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
        LabelComp name = new LabelComp(wrapper.nameProperty());
        name.apply(struc -> struc.get().setTextOverrun(OverrunStyle.CENTER_ELLIPSIS))
                .apply(struc -> struc.get().setPadding(new Insets(5, 5, 5, 0)));
        name.apply(s -> AppFont.header(s.get()));
        name.styleClass("name");
        return name;
    }

    protected Node createIcon(int w, int h) {
        var img = wrapper.disabledProperty().get()
                ? "disabled_icon.png"
                : wrapper.getEntry()
                        .getProvider()
                        .getDisplayIconFileName(wrapper.getEntry().getStore());
        var imageComp = PrettyImageHelper.ofFixedSize(img, w, h);
        var storeIcon = imageComp.createRegion();
        if (wrapper.getValidity().getValue().isUsable()) {
            new FancyTooltipAugment<>(new SimpleStringProperty(
                            wrapper.getEntry().getProvider().getDisplayName()))
                    .augment(storeIcon);
        }

        var stack = new StackPane(storeIcon);
        stack.setMinHeight(w + 7);
        stack.setMinWidth(w + 7);
        stack.setMaxHeight(w + 7);
        stack.setMaxWidth(w + 7);
        stack.getStyleClass().add("icon");
        stack.setAlignment(Pos.CENTER);
        return stack;
    }

    protected Comp<?> createButtonBar() {
        var list = new ArrayList<Comp<?>>();
        for (var p : wrapper.getActionProviders().entrySet()) {
            var actionProvider = p.getKey().getDataStoreCallSite();
            if (!actionProvider.isMajor(wrapper.getEntry().ref())) {
                continue;
            }

            var def = p.getKey().getDefaultDataStoreCallSite();
            if (def != null && def.equals(wrapper.getDefaultActionProvider().getValue())) {
                continue;
            }

            var button = new IconButtonComp(
                    actionProvider.getIcon(wrapper.getEntry().ref()), () -> {
                        ThreadHelper.runFailableAsync(() -> {
                            var action = actionProvider.createAction(
                                    wrapper.getEntry().ref());
                            action.execute();
                        });
                    });
            button.accessibleText(actionProvider.getName(wrapper.getEntry().ref()).getValue());
            button.apply(new FancyTooltipAugment<>(
                    actionProvider.getName(wrapper.getEntry().ref())));
            if (actionProvider.activeType() == ActionProvider.DataStoreCallSite.ActiveType.ONLY_SHOW_IF_ENABLED) {
                button.hide(Bindings.not(p.getValue()));
            } else if (actionProvider.activeType() == ActionProvider.DataStoreCallSite.ActiveType.ALWAYS_SHOW) {
                button.disable(Bindings.not(p.getValue()));
            }
            list.add(button);
        }

        var settingsButton = createSettingsButton();
        list.add(settingsButton);
        if (list.size() > 1) {
            list.getFirst().styleClass(Styles.LEFT_PILL);
            for (int i = 1; i < list.size() - 1; i++) {
                list.get(i).styleClass(Styles.CENTER_PILL);
            }
            list.getLast().styleClass(Styles.RIGHT_PILL);
        }
        list.forEach(comp -> {
            comp.apply(struc -> struc.get().getStyleClass().remove(Styles.FLAT));
        });
        return new HorizontalComp(list)
                .apply(struc -> {
                    struc.get().setAlignment(Pos.CENTER_RIGHT);
                    struc.get().setPadding(new Insets(5));
                })
                .styleClass("button-bar");
    }

    protected Comp<?> createSettingsButton() {
        var settingsButton = new IconButtonComp("mdi2d-dots-horizontal-circle-outline", () -> {});
        settingsButton.styleClass("settings");
        settingsButton.accessibleText("More");
        settingsButton.apply(new ContextMenuAugment<>(
                event -> event.getButton() == MouseButton.PRIMARY, () -> StoreEntryComp.this.createContextMenu()));
        settingsButton.apply(new FancyTooltipAugment<>("more"));
        return settingsButton;
    }

    protected ContextMenu createContextMenu() {
        var contextMenu = new ContextMenu();
        AppFont.normal(contextMenu.getStyleableNode());

        var hasSep = false;
        for (var p : wrapper.getActionProviders().entrySet()) {
            var actionProvider = p.getKey().getDataStoreCallSite();
            if (actionProvider.isMajor(wrapper.getEntry().ref())) {
                continue;
            }

            if (actionProvider.isSystemAction() && !hasSep) {
                if (contextMenu.getItems().size() > 0) {
                    contextMenu.getItems().add(new SeparatorMenuItem());
                }
                hasSep = true;
            }

            var name = actionProvider.getName(wrapper.getEntry().ref());
            var icon = actionProvider.getIcon(wrapper.getEntry().ref());
            var item = actionProvider.canLinkTo()
                    ? new Menu(null, new FontIcon(icon))
                    : new MenuItem(null, new FontIcon(icon));

            var proRequired = p.getKey().getProFeatureId() != null &&
                    !LicenseProvider.get().getFeature(p.getKey().getProFeatureId()).isSupported();
            if (proRequired) {
                item.setDisable(true);
                item.textProperty().bind(Bindings.createStringBinding(() -> name.getValue() + " (Pro)",name));
            } else {
                item.textProperty().bind(name);
            }

            Menu menu = actionProvider.canLinkTo() ? (Menu) item : null;
            item.setOnAction(event -> {
                if (menu != null && !event.getTarget().equals(menu)) {
                    return;
                }

                if (menu != null && menu.isDisable()) {
                    return;
                }

                contextMenu.hide();
                ThreadHelper.runFailableAsync(() -> {
                    var action = actionProvider.createAction(
                            wrapper.getEntry().ref());
                    action.execute();
                });
            });
            if (actionProvider.activeType() == ActionProvider.DataStoreCallSite.ActiveType.ONLY_SHOW_IF_ENABLED) {
                item.visibleProperty().bind(p.getValue());
            } else if (actionProvider.activeType() == ActionProvider.DataStoreCallSite.ActiveType.ALWAYS_SHOW) {
                item.disableProperty().bind(Bindings.not(p.getValue()));
            }
            contextMenu.getItems().add(item);

            if (menu != null) {
                var run = new MenuItem(null, new FontIcon("mdi2c-code-greater-than"));
                run.textProperty().bind(AppI18n.observable("base.execute"));
                run.setOnAction(event -> {
                    ThreadHelper.runFailableAsync(() -> {
                        p.getKey().getDataStoreCallSite().createAction(wrapper.getEntry().ref()).execute();
                    });
                });
                menu.getItems().add(run);


                var sc = new MenuItem(null, new FontIcon("mdi2c-code-greater-than"));
                var url = "xpipe://action/" + p.getKey().getId() + "/"
                        + wrapper.getEntry().getUuid();
                sc.textProperty().bind(AppI18n.observable("base.createShortcut"));
                sc.setOnAction(event -> {
                    ThreadHelper.runFailableAsync(() -> {
                        DesktopShortcuts.create(url,
                                wrapper.nameProperty().getValue() + " (" + p.getKey().getDataStoreCallSite().getName(wrapper.getEntry().ref()).getValue() + ")");
                    });
                });
                menu.getItems().add(sc);

                if (XPipeDistributionType.get().isSupportsUrls()) {
                    var l = new MenuItem(null, new FontIcon("mdi2c-clipboard-list-outline"));
                    l.textProperty().bind(AppI18n.observable("base.copyShareLink"));
                    l.setOnAction(event -> {
                        ThreadHelper.runFailableAsync(() -> {
                            AppActionLinkDetector.setLastDetectedAction(url);
                            ClipboardHelper.copyUrl(url);
                        });
                    });
                    menu.getItems().add(l);
                }
            }
        }

        if (contextMenu.getItems().size() > 0 && !hasSep) {
            contextMenu.getItems().add(new SeparatorMenuItem());
        }

        if (AppPrefs.get().developerMode().getValue()) {
            var browse = new MenuItem(AppI18n.get("browseInternalStorage"), new FontIcon("mdi2f-folder-open-outline"));
            browse.setOnAction(
                    event -> DesktopHelper.browsePath(wrapper.getEntry().getDirectory()));
            contextMenu.getItems().add(browse);
        }

        if (wrapper.getEntry().getProvider() != null && wrapper.getEntry().getProvider().canMoveCategories()) {
            var move = new Menu(AppI18n.get("moveTo"), new FontIcon("mdi2f-folder-move-outline"));
            StoreViewState.get().getSortedCategories(wrapper.getCategory().getValue().getRoot()).forEach(storeCategoryWrapper -> {
                MenuItem m = new MenuItem(storeCategoryWrapper.getName());
                m.setOnAction(event -> {
                    wrapper.moveTo(storeCategoryWrapper.getCategory());
                    event.consume();
                });
                if (storeCategoryWrapper.getParent() == null) {
                    m.setDisable(true);
                }

                move.getItems().add(m);
            });
            contextMenu.getItems().add(move);
        }

        if (DataStorage.get().isRootEntry(wrapper.getEntry())) {
            var color = new Menu(AppI18n.get("color"), new FontIcon("mdi2f-format-color-fill"));
            var none = new MenuItem("None");
            none.setOnAction(event -> {
                wrapper.getEntry().setColor(null);
                event.consume();
            });
            color.getItems().add(none);
            Arrays.stream(DataStoreColor.values()).forEach(dataStoreColor -> {
                MenuItem m = new MenuItem(DataStoreFormatter.capitalize(dataStoreColor.getId()));
                m.setOnAction(event -> {
                    wrapper.getEntry().setColor(dataStoreColor);
                    event.consume();
                });
                color.getItems().add(m);
            });
            contextMenu.getItems().add(color);
        }

        var del = new MenuItem(AppI18n.get("remove"), new FontIcon("mdal-delete_outline"));
        del.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            return !wrapper.getDeletable().get() && !AppPrefs.get().developerDisableGuiRestrictions().get();
        }, wrapper.getDeletable(), AppPrefs.get().developerDisableGuiRestrictions()));
        del.setOnAction(event -> wrapper.delete());
        contextMenu.getItems().add(del);

        return contextMenu;
    }

    protected ColumnConstraints createShareConstraint(Region r, double share) {
        var cc = new ColumnConstraints();
        cc.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> r.getWidth() * share, r.widthProperty()));
        return cc;
    }
}
