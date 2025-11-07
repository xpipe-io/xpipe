package io.xpipe.app.browser;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.LoadingIconComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.comp.base.StackComp;
import io.xpipe.app.core.App;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.ContextMenuHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.BooleanScope;

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

    public BrowserSessionTabsComp(
            BrowserFullSessionModel model, ObservableDoubleValue leftPadding, DoubleProperty rightPadding) {
        this.model = model;
        this.leftPadding = leftPadding;
        this.rightPadding = rightPadding;
        this.headerHeight = new SimpleDoubleProperty();
    }

    private static void setupKeyEvents(TabPane tabs) {
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
    }

    public Region createSimple() {
        var tabs = createTabPane();
        var topBackground = Comp.hspacer().styleClass("top-spacer").createRegion();
        leftPadding.subscribe(number -> {
            StackPane.setMargin(topBackground, new Insets(0, 0, 0, -number.doubleValue() - 3));
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

        setupCustomStyle(tabs);
        // Sync to guarantee that no external changes are made during this
        synchronized (model) {
            setupTabEntries(tabs);
        }
        setupKeyEvents(tabs);

        return tabs;
    }

    private void setupTabEntries(TabPane tabs) {
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
                        try (var ignored = new BooleanScope(addingTab).start()) {
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
    }

    private void setupCustomStyle(TabPane tabs) {
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
                        node.setPickOnBounds(false);

                        var r = (Region) node;
                        r.prefHeightProperty().bind(r.maxHeightProperty());
                        r.setMinHeight(Region.USE_PREF_SIZE);
                    });

                    Region headerArea = (Region) tabs.lookup(".tab-header-area");
                    headerArea
                            .paddingProperty()
                            .bind(Bindings.createObjectBinding(
                                    () -> {
                                        var w = App.getApp().getStage().getWidth();
                                        if (w >= 1000) {
                                            return new Insets(2, 0, 4, -leftPadding.get() + 3);
                                        } else {
                                            return new Insets(2, 0, 4, -leftPadding.get() - 4);
                                        }
                                    },
                                    App.getApp().getStage().widthProperty(),
                                    leftPadding));
                    tabs.paddingProperty()
                            .bind(Bindings.createObjectBinding(
                                    () -> {
                                        var w = App.getApp().getStage().getWidth();
                                        if (w >= 1000) {
                                            return new Insets(0, 0, 0, -5);
                                        } else {
                                            return new Insets(0, 0, 0, 5);
                                        }
                                    },
                                    App.getApp().getStage().widthProperty()));
                    headerHeight.bind(headerArea.heightProperty());
                });
            }
        });
    }

    private ContextMenu createContextMenu(TabPane tabs, Tab tab, BrowserSessionTab tabModel) {
        var cm = ContextMenuHelper.create();

        if (tabModel.isCloseable()) {
            var unpin = ContextMenuHelper.item(LabelGraphic.none(), "unpinTab");
            unpin.visibleProperty()
                    .bind(PlatformThread.sync(Bindings.createBooleanBinding(
                            () -> {
                                return model.getGlobalPinnedTab().getValue() != null
                                        && model.getGlobalPinnedTab().getValue().equals(tabModel);
                            },
                            model.getGlobalPinnedTab())));
            unpin.setOnAction(event -> {
                model.unpinTab();
                event.consume();
            });
            cm.getItems().add(unpin);

            var pin = ContextMenuHelper.item(LabelGraphic.none(), "pinTab");
            pin.visibleProperty()
                    .bind(PlatformThread.sync(Bindings.createBooleanBinding(
                            () -> {
                                return model.getGlobalPinnedTab().getValue() == null;
                            },
                            model.getGlobalPinnedTab())));
            pin.setOnAction(event -> {
                model.pinTab(tabModel);
                event.consume();
            });
            cm.getItems().add(pin);
        }

        var select = ContextMenuHelper.item(LabelGraphic.none(), "selectTab");
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

        var close = ContextMenuHelper.item(LabelGraphic.none(), "closeTab");
        close.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
        close.setOnAction(event -> {
            if (tab.isClosable()) {
                tabs.getTabs().remove(tab);
            }
            event.consume();
        });
        cm.getItems().add(close);

        var closeOthers = ContextMenuHelper.item(LabelGraphic.none(), "closeOtherTabs");
        closeOthers.setOnAction(event -> {
            tabs.getTabs()
                    .removeAll(tabs.getTabs().stream()
                            .filter(t -> t != tab && t.isClosable())
                            .toList());
            event.consume();
        });
        cm.getItems().add(closeOthers);

        var closeLeft = ContextMenuHelper.item(LabelGraphic.none(), "closeLeftTabs");
        closeLeft.setOnAction(event -> {
            var index = tabs.getTabs().indexOf(tab);
            tabs.getTabs()
                    .removeAll(tabs.getTabs().stream()
                            .filter(t -> tabs.getTabs().indexOf(t) < index && t.isClosable())
                            .toList());
            event.consume();
        });
        cm.getItems().add(closeLeft);

        var closeRight = ContextMenuHelper.item(LabelGraphic.none(), "closeRightTabs");
        closeRight.setOnAction(event -> {
            var index = tabs.getTabs().indexOf(tab);
            tabs.getTabs()
                    .removeAll(tabs.getTabs().stream()
                            .filter(t -> tabs.getTabs().indexOf(t) > index && t.isClosable())
                            .toList());
            event.consume();
        });
        cm.getItems().add(closeRight);

        var closeAll = ContextMenuHelper.item(LabelGraphic.none(), "closeAllTabs");
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
        if (tabModel.isCloseable()) {
            tab.setContextMenu(createContextMenu(tabs, tab, tabModel));
        }

        tab.setClosable(tabModel.isCloseable());
        // Prevent closing while busy
        tab.setOnCloseRequest(event -> {
            if (!tabModel.canImmediatelyClose()) {
                event.consume();
            }
        });

        if (tabModel.getIcon() != null) {
            var loading = new LoadingIconComp(tabModel.getBusy(), AppFontSizes::base);
            loading.prefWidth(16);
            loading.prefHeight(16);

            var image = tabModel.getIcon();
            var logo = PrettyImageHelper.ofFixedSizeSquare(image, 16);
            logo.apply(struc -> {
                struc.get()
                        .opacityProperty()
                        .bind(PlatformThread.sync(Bindings.createDoubleBinding(
                                () -> {
                                    return !tabModel.getBusy().get() ? 1.0 : 0.15;
                                },
                                tabModel.getBusy())));
            });

            var stack = new StackComp(List.of(logo, loading));
            tab.setGraphic(stack.createRegion());
        }

        if (tabModel.getBrowserModel() instanceof BrowserFullSessionModel sessionModel) {
            var global = PlatformThread.sync(sessionModel.getGlobalPinnedTab());
            tab.textProperty()
                    .bind(Bindings.createStringBinding(
                            () -> {
                                var n = tabModel.getName().getValue();
                                return (AppPrefs.get().censorMode().get() ? "*".repeat(n.length()) : n)
                                        + (global.getValue() == tabModel ? " (" + AppI18n.get("pinned") + ")" : "");
                            },
                            tabModel.getName(),
                            global,
                            AppI18n.activeLanguage(),
                            AppPrefs.get().censorMode()));
        } else {
            tab.textProperty().bind(tabModel.getName());
        }

        Comp<?> comp = tabModel.comp();
        var compRegion = comp.createRegion();

        var empty = new StackPane();
        empty.setMinWidth(180);
        empty.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (tabModel.isCloseable() && tabs.getSelectionModel().getSelectedItem() == tab) {
                rightPadding.setValue(newValue.doubleValue());
            }
        });

        var split = new SplitPane(compRegion);
        if (tabModel.isCloseable()) {
            split.getItems().add(empty);
        }
        tabs.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (tabModel.isCloseable() && newValue == tab) {
                rightPadding.setValue(empty.getWidth());
            }
        });
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
                    l.setGraphicTextGap(7);
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
                    } else {
                        c.getStyleClass().add("gray");
                    }
                    c.addEventHandler(DragEvent.DRAG_ENTERED, de -> {
                        // Prevent switch when dragging local files into app
                        if (tabModel.isCloseable() && !de.getDragboard().hasContent(DataFormat.FILES)) {
                            Platform.runLater(() -> tabs.getSelectionModel().select(tab));
                            de.consume();
                        }
                    });
                });
            }
        });

        return tab;
    }
}
