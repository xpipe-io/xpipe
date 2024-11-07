package io.xpipe.app.browser;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ContextMenuHelper;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.input.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.theme.Styles;
import lombok.Getter;

import java.util.*;

import static atlantafx.base.theme.Styles.DENSE;
import static atlantafx.base.theme.Styles.toggleStyleClass;
import static javafx.scene.control.TabPane.TabClosingPolicy.ALL_TABS;

public class BrowserSessionTabsComp extends SimpleComp {

    private final BrowserFullSessionModel model;
    private final ObservableDoubleValue leftPadding;
    private final DoubleProperty rightPadding;

    @Getter
    private final DoubleProperty headerHeight;

    public BrowserSessionTabsComp(BrowserFullSessionModel model, ObservableDoubleValue leftPadding, DoubleProperty rightPadding) {
        this.model = model;
        this.leftPadding = leftPadding;
        this.rightPadding = rightPadding;
        this.headerHeight = new SimpleDoubleProperty();
    }

    public Region createSimple() {
        var tabs = createTabPane();
        var topBackground = Comp.hspacer().styleClass("top-spacer").createRegion();
        leftPadding.subscribe(number -> {
            StackPane.setMargin(topBackground, new Insets(0, 0, 0, -number.doubleValue()));
        });
        var stack = new StackPane(topBackground, tabs);
        stack.setAlignment(Pos.TOP_CENTER);
        topBackground.prefHeightProperty().bind(headerHeight);
        topBackground.maxHeightProperty().bind(topBackground.prefHeightProperty());
        topBackground.prefWidthProperty().bind(tabs.widthProperty());
        return stack;
    }

    private TabPane createTabPane() {
        var tabs = new TabPane();
        tabs.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        tabs.setTabMinWidth(Region.USE_PREF_SIZE);
        tabs.setTabMaxWidth(400);
        tabs.setTabClosingPolicy(ALL_TABS);
        tabs.setSkin(new TabPaneSkin(tabs));
        Styles.toggleStyleClass(tabs, TabPane.STYLE_CLASS_FLOATING);
        toggleStyleClass(tabs, DENSE);

        tabs.skinProperty().subscribe(newValue -> {
            if (newValue != null) {
                Platform.runLater(() -> {
                    tabs.setClip(null);
                    tabs.setPickOnBounds(false);
                    tabs.lookupAll(".tab-header-area").forEach(node -> {
                        node.setClip(null);
                        node.setPickOnBounds(false);

                        var r = (Region) node;
                        r.prefHeightProperty().bind(r.maxHeightProperty());
                        r.setMinHeight(Region.USE_PREF_SIZE);
                    });
                    tabs.lookupAll(".headers-region").forEach(node -> {
                        node.setClip(null);
                        node.setPickOnBounds(false);

                        var r = (Region) node;
                        r.prefHeightProperty().bind(r.maxHeightProperty());
                        r.setMinHeight(Region.USE_PREF_SIZE);
                    });

                    Region headerArea = (Region) tabs.lookup(".tab-header-area");
                    headerArea
                            .paddingProperty()
                            .bind(Bindings.createObjectBinding(
                                    () -> new Insets(2, 0, 4, -leftPadding.get() + 2), leftPadding));
                    headerHeight.bind(headerArea.heightProperty());
                });
            }
        });

        var map = new HashMap<BrowserSessionTab, Tab>();

        // Restore state
        model.getSessionEntries().forEach(v -> {
            var t = createTab(tabs, v);
            map.put(v, t);
            tabs.getTabs().add(t);
        });
        tabs.getSelectionModel()
                .select(model.getSessionEntries()
                        .indexOf(model.getSelectedEntry().getValue()));

        // Used for ignoring changes by the tabpane when new tabs are added. We want to perform the selections manually!
        var addingTab = new SimpleBooleanProperty();

        // Handle selection from platform
        tabs.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (addingTab.get()) {
                return;
            }

            if (newValue == null) {
                model.getSelectedEntry().setValue(null);
                return;
            }

            var source = map.entrySet().stream()
                    .filter(openFileSystemModelTabEntry ->
                            openFileSystemModelTabEntry.getValue().equals(newValue))
                    .findAny()
                    .map(Map.Entry::getKey)
                    .orElse(null);
            model.getSelectedEntry().setValue(source);
        });

        // Handle selection from model
        model.getSelectedEntry().addListener((observable, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (newValue == null) {
                    tabs.getSelectionModel().select(null);
                    return;
                }

                var toSelect = map.entrySet().stream()
                        .filter(openFileSystemModelTabEntry ->
                                openFileSystemModelTabEntry.getKey().equals(newValue))
                        .findAny()
                        .map(Map.Entry::getValue)
                        .orElse(null);
                if (toSelect == null || !tabs.getTabs().contains(toSelect)) {
                    tabs.getSelectionModel().select(null);
                    return;
                }

                tabs.getSelectionModel().select(toSelect);
                Platform.runLater(() -> {
                    toSelect.getContent().requestFocus();
                });
            });
        });

        model.getSessionEntries().addListener((ListChangeListener<? super BrowserSessionTab>) c -> {
            while (c.next()) {
                for (var r : c.getRemoved()) {
                    PlatformThread.runLaterIfNeeded(() -> {
                        var t = map.remove(r);
                        tabs.getTabs().remove(t);
                    });
                }

                for (var a : c.getAddedSubList()) {
                    PlatformThread.runLaterIfNeeded(() -> {
                        try (var b = new BooleanScope(addingTab).start()) {
                            var t = createTab(tabs, a);
                            map.put(a, t);
                            tabs.getTabs().add(t);
                        }
                    });
                }
            }
        });

        tabs.getTabs().addListener((ListChangeListener<? super Tab>) c -> {
            while (c.next()) {
                for (var r : c.getRemoved()) {
                    var source = map.entrySet().stream()
                            .filter(openFileSystemModelTabEntry ->
                                    openFileSystemModelTabEntry.getValue().equals(r))
                            .findAny()
                            .orElse(null);

                    // Only handle close events that are triggered from the platform
                    if (source == null) {
                        continue;
                    }

                    model.closeAsync(source.getKey());
                }
            }
        });

        tabs.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
            var current = tabs.getSelectionModel().getSelectedItem();
            if (current == null) {
                return;
            }

            if (new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN).match(keyEvent)) {
                tabs.getTabs().remove(current);
                keyEvent.consume();
                return;
            }

            if (new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
                    .match(keyEvent)) {
                tabs.getTabs().clear();
                keyEvent.consume();
            }

            if (keyEvent.getCode().isFunctionKey()) {
                var start = KeyCode.F1.getCode();
                var index = keyEvent.getCode().getCode() - start;
                if (index < tabs.getTabs().size()) {
                    tabs.getSelectionModel().select(index);
                    keyEvent.consume();
                    return;
                }
            }

            var forward = new KeyCodeCombination(KeyCode.TAB, KeyCombination.SHORTCUT_DOWN);
            if (forward.match(keyEvent)) {
                var next = (tabs.getSelectionModel().getSelectedIndex() + 1)
                        % tabs.getTabs().size();
                tabs.getSelectionModel().select(next);
                keyEvent.consume();
                return;
            }

            var back = new KeyCodeCombination(KeyCode.TAB, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
            if (back.match(keyEvent)) {
                var previous = (tabs.getTabs().size() + tabs.getSelectionModel().getSelectedIndex() - 1)
                        % tabs.getTabs().size();
                tabs.getSelectionModel().select(previous);
                keyEvent.consume();
            }
        });

        return tabs;
    }

    private ContextMenu createContextMenu(TabPane tabs, Tab tab, BrowserSessionTab tabModel) {
        var cm = ContextMenuHelper.create();

        if (tabModel.isCloseable()) {
            var unsplit = ContextMenuHelper.item(LabelGraphic.none(), AppI18n.get("unpinTab"));
            unsplit.visibleProperty().bind(PlatformThread.sync(Bindings.createBooleanBinding(() -> {
                return model.getGlobalPinnedTab().getValue() != null && model.getGlobalPinnedTab().getValue().equals(tabModel);
            }, model.getGlobalPinnedTab())));
            unsplit.setOnAction(event -> {
                model.unpinTab(tabModel);
                event.consume();
            });
            cm.getItems().add(unsplit);

            var split = ContextMenuHelper.item(LabelGraphic.none(), AppI18n.get("pinTab"));
            split.setOnAction(event -> {
                model.pinTab(tabModel);
                event.consume();
            });
            cm.getItems().add(split);
        }

        var select = ContextMenuHelper.item(LabelGraphic.none(), AppI18n.get("selectTab"));
        select.acceleratorProperty()
                .bind(Bindings.createObjectBinding(
                        () -> {
                            var start = KeyCode.F1.getCode();
                            var index = tabs.getTabs().indexOf(tab);
                            var keyCode = Arrays.stream(KeyCode.values())
                                    .filter(code -> code.getCode() == start + index)
                                    .findAny()
                                    .orElse(null);
                            return keyCode != null ? new KeyCodeCombination(keyCode) : null;
                        },
                        tabs.getTabs()));
        select.setOnAction(event -> {
            tabs.getSelectionModel().select(tab);
            event.consume();
        });
        cm.getItems().add(select);

        cm.getItems().add(new SeparatorMenuItem());

        var close = ContextMenuHelper.item(LabelGraphic.none(), AppI18n.get("closeTab"));
        close.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
        close.setOnAction(event -> {
            if (tab.isClosable()) {
                tabs.getTabs().remove(tab);
            }
            event.consume();
        });
        cm.getItems().add(close);

        var closeOthers = ContextMenuHelper.item(LabelGraphic.none(), AppI18n.get("closeOtherTabs"));
        closeOthers.setOnAction(event -> {
            tabs.getTabs()
                    .removeAll(tabs.getTabs().stream()
                            .filter(t -> t != tab && t.isClosable())
                            .toList());
            event.consume();
        });
        cm.getItems().add(closeOthers);

        var closeLeft = ContextMenuHelper.item(LabelGraphic.none(), AppI18n.get("closeLeftTabs"));
        closeLeft.setOnAction(event -> {
            var index = tabs.getTabs().indexOf(tab);
            tabs.getTabs()
                    .removeAll(tabs.getTabs().stream()
                            .filter(t -> tabs.getTabs().indexOf(t) < index && t.isClosable())
                            .toList());
            event.consume();
        });
        cm.getItems().add(closeLeft);

        var closeRight = ContextMenuHelper.item(LabelGraphic.none(), AppI18n.get("closeRightTabs"));
        closeRight.setOnAction(event -> {
            var index = tabs.getTabs().indexOf(tab);
            tabs.getTabs()
                    .removeAll(tabs.getTabs().stream()
                            .filter(t -> tabs.getTabs().indexOf(t) > index && t.isClosable())
                            .toList());
            event.consume();
        });
        cm.getItems().add(closeRight);

        var closeAll = ContextMenuHelper.item(LabelGraphic.none(), AppI18n.get("closeAllTabs"));
        closeAll.setAccelerator(
                new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        closeAll.setOnAction(event -> {
            tabs.getTabs()
                    .removeAll(
                            tabs.getTabs().stream().filter(t -> t.isClosable()).toList());
            event.consume();
        });
        cm.getItems().add(closeAll);

        return cm;
    }

    private Tab createTab(TabPane tabs, BrowserSessionTab tabModel) {
        var tab = new Tab();
        tab.setContextMenu(createContextMenu(tabs, tab, tabModel));

        tab.setClosable(tabModel.isCloseable());
        // Prevent closing while busy
        tab.setOnCloseRequest(event -> {
            if (!tabModel.canImmediatelyClose()) {
                event.consume();
            }
        });

        if (tabModel.getIcon() != null) {
            var ring = new RingProgressIndicator(0, false);
            ring.setMinSize(16, 16);
            ring.setPrefSize(16, 16);
            ring.setMaxSize(16, 16);
            ring.progressProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> tabModel.getBusy().get()
                                            && !AppPrefs.get().performanceMode().get()
                                    ? -1d
                                    : 0,
                            PlatformThread.sync(tabModel.getBusy()),
                            AppPrefs.get().performanceMode()));

            var image = tabModel.getIcon();
            var logo = PrettyImageHelper.ofFixedSizeSquare(image, 16).createRegion();

            tab.graphicProperty()
                    .bind(Bindings.createObjectBinding(
                            () -> {
                                return tabModel.getBusy().get() ? ring : logo;
                            },
                            PlatformThread.sync(tabModel.getBusy())));
        }

        if (tabModel.getBrowserModel() instanceof BrowserFullSessionModel sessionModel) {
            var global = PlatformThread.sync(sessionModel.getGlobalPinnedTab());
            tab.textProperty().bind(Bindings.createStringBinding(() -> {
                return tabModel.getName() + (global.getValue() == tabModel ? " (" + AppI18n.get("pinned") + ")" : "");
            }, global, AppPrefs.get().language()));
        } else {
            tab.setText(tabModel.getName());
        }

        Comp<?> comp = tabModel.comp();
        var compRegion = comp.createRegion();
        var empty = new StackPane();
        empty.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (tabModel.isCloseable() && tabs.getSelectionModel().getSelectedItem() == tab) {
                rightPadding.setValue(newValue.doubleValue());
            }
        });
        var split = new SplitPane(compRegion);
        if (tabModel.isCloseable()) {
            split.getItems().add(empty);
        }
        model.getEffectiveRightTab().subscribe(browserSessionTab -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (browserSessionTab != null && split.getItems().size() > 1) {
                    split.getItems().set(1, empty);
                } else if (browserSessionTab != null && split.getItems().size() == 1) {
                    split.getItems().add(empty);
                } else if (browserSessionTab == null && split.getItems().size() > 1) {
                    split.getItems().remove(1);
                }
            });
        });
        tab.setContent(split);

        var id = UUID.randomUUID().toString();
        tab.setId(id);

        tabs.skinProperty().subscribe(newValue -> {
            if (newValue != null) {
                Platform.runLater(() -> {
                    Label l = (Label) tabs.lookup("#" + id + " .tab-label");
                    var w = l.maxWidthProperty();
                    l.minWidthProperty().bind(w);
                    l.prefWidthProperty().bind(w);
                    if (!tabModel.isCloseable()) {
                        l.pseudoClassStateChanged(PseudoClass.getPseudoClass("static"), true);
                    }

                    var close = (StackPane) tabs.lookup("#" + id + " .tab-close-button");
                    close.setPrefWidth(30);

                    StackPane c = (StackPane) tabs.lookup("#" + id + " .tab-container");
                    c.getStyleClass().add("color-box");
                    var color = tabModel.getColor();
                    if (color != null) {
                        c.getStyleClass().add(color.getId());
                    }
                    c.addEventHandler(
                            DragEvent.DRAG_ENTERED,
                            mouseEvent -> Platform.runLater(
                                    () -> tabs.getSelectionModel().select(tab)));
                });
            }
        });

        return tab;
    }
}
