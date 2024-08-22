package io.xpipe.app.browser.session;

import io.xpipe.app.browser.BrowserWelcomeComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.fxcomps.impl.TooltipAugment;
import io.xpipe.app.fxcomps.util.LabelGraphic;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ContextMenuHelper;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.theme.Styles;

import java.util.*;

import static atlantafx.base.theme.Styles.DENSE;
import static atlantafx.base.theme.Styles.toggleStyleClass;
import static javafx.scene.control.TabPane.TabClosingPolicy.ALL_TABS;

public class BrowserSessionTabsComp extends SimpleComp {

    private final BrowserSessionModel model;
    private final ObservableDoubleValue leftPadding;

    public BrowserSessionTabsComp(BrowserSessionModel model, ObservableDoubleValue leftPadding) {
        this.model = model;
        this.leftPadding = leftPadding;
    }

    public Region createSimple() {
        var map = new LinkedHashMap<Comp<?>, ObservableValue<Boolean>>();
        map.put(Comp.hspacer().styleClass("top-spacer"), new SimpleBooleanProperty(true));
        map.put(Comp.of(() -> createTabPane()), Bindings.isNotEmpty(model.getSessionEntries()));
        map.put(
                new BrowserWelcomeComp(model).apply(struc -> StackPane.setAlignment(struc.get(), Pos.CENTER_LEFT)),
                Bindings.createBooleanBinding(
                        () -> {
                            return model.getSessionEntries().size() == 0;
                        },
                        model.getSessionEntries()));
        var multi = new MultiContentComp(map);
        multi.apply(struc -> ((StackPane) struc.get()).setAlignment(Pos.TOP_CENTER));
        return multi.createRegion();
    }

    private TabPane createTabPane() {
        var tabs = new TabPane();
        tabs.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        tabs.setTabMinWidth(Region.USE_PREF_SIZE);
        tabs.setTabMaxWidth(400);
        tabs.setTabClosingPolicy(ALL_TABS);
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
                    });
                    tabs.lookupAll(".headers-region").forEach(node -> {
                        node.setClip(null);
                        node.setPickOnBounds(false);
                    });

                    Region headerArea = (Region) tabs.lookup(".tab-header-area");
                    headerArea
                            .paddingProperty()
                            .bind(Bindings.createObjectBinding(
                                    () -> new Insets(0, 0, 0, -leftPadding.get() + 2), leftPadding));
                });
            }
        });

        var map = new HashMap<BrowserSessionTab<?>, Tab>();

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

        model.getSessionEntries().addListener((ListChangeListener<? super BrowserSessionTab<?>>) c -> {
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
                return;
            }
        });

        return tabs;
    }

    private ContextMenu createContextMenu(TabPane tabs, Tab tab) {
        var cm = ContextMenuHelper.create();

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
            tabs.getTabs().remove(tab);
            event.consume();
        });
        cm.getItems().add(close);

        var closeOthers = ContextMenuHelper.item(LabelGraphic.none(), AppI18n.get("closeOtherTabs"));
        closeOthers.setOnAction(event -> {
            tabs.getTabs()
                    .removeAll(tabs.getTabs().stream().filter(t -> t != tab).toList());
            event.consume();
        });
        cm.getItems().add(closeOthers);

        var closeLeft = ContextMenuHelper.item(LabelGraphic.none(), AppI18n.get("closeLeftTabs"));
        closeLeft.setOnAction(event -> {
            var index = tabs.getTabs().indexOf(tab);
            tabs.getTabs()
                    .removeAll(tabs.getTabs().stream()
                            .filter(t -> tabs.getTabs().indexOf(t) < index)
                            .toList());
            event.consume();
        });
        cm.getItems().add(closeLeft);

        var closeRight = ContextMenuHelper.item(LabelGraphic.none(), AppI18n.get("closeRightTabs"));
        closeRight.setOnAction(event -> {
            var index = tabs.getTabs().indexOf(tab);
            tabs.getTabs()
                    .removeAll(tabs.getTabs().stream()
                            .filter(t -> tabs.getTabs().indexOf(t) > index)
                            .toList());
            event.consume();
        });
        cm.getItems().add(closeRight);

        var closeAll = ContextMenuHelper.item(LabelGraphic.none(), AppI18n.get("closeAllTabs"));
        closeAll.setAccelerator(
                new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        closeAll.setOnAction(event -> {
            tabs.getTabs().clear();
            event.consume();
        });
        cm.getItems().add(closeAll);

        return cm;
    }

    private Tab createTab(TabPane tabs, BrowserSessionTab<?> model) {
        var tab = new Tab();
        tab.setContextMenu(createContextMenu(tabs, tab));

        var ring = new RingProgressIndicator(0, false);
        ring.setMinSize(16, 16);
        ring.setPrefSize(16, 16);
        ring.setMaxSize(16, 16);
        ring.progressProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> model.getBusy().get() ? -1d : 0, PlatformThread.sync(model.getBusy())));

        var image = model.getEntry()
                .get()
                .getProvider()
                .getDisplayIconFileName(model.getEntry().getStore());
        var logo = PrettyImageHelper.ofFixedSizeSquare(image, 16).createRegion();

        tab.graphicProperty()
                .bind(Bindings.createObjectBinding(
                        () -> {
                            return model.getBusy().get() ? ring : logo;
                        },
                        PlatformThread.sync(model.getBusy())));
        tab.setText(model.getName());

        Comp<?> comp = model.comp();
        tab.setContent(comp.createRegion());

        var id = UUID.randomUUID().toString();
        tab.setId(id);

        tabs.skinProperty().subscribe(newValue -> {
            if (newValue != null) {
                Platform.runLater(() -> {
                    Label l = (Label) tabs.lookup("#" + id + " .tab-label");
                    var w = l.maxWidthProperty();
                    l.minWidthProperty().bind(w);
                    l.prefWidthProperty().bind(w);

                    var close = (StackPane) tabs.lookup("#" + id + " .tab-close-button");
                    close.setPrefWidth(30);

                    StackPane c = (StackPane) tabs.lookup("#" + id + " .tab-container");
                    c.getStyleClass().add("color-box");
                    var color = DataStorage.get().getEffectiveColor(model.getEntry().get());
                    if (color != null) {
                        c.getStyleClass().add(color.getId());
                    }
                    new TooltipAugment<>(new SimpleStringProperty(model.getTooltip()), null).augment(c);
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
