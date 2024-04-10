package io.xpipe.app.browser.session;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.theme.Styles;
import io.xpipe.app.browser.BrowserWelcomeComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.fxcomps.impl.TooltipAugment;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.BooleanScope;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static atlantafx.base.theme.Styles.DENSE;
import static atlantafx.base.theme.Styles.toggleStyleClass;
import static javafx.scene.control.TabPane.TabClosingPolicy.ALL_TABS;

public class BrowserSessionTabsComp extends SimpleComp {

    private final BrowserSessionModel model;

    public BrowserSessionTabsComp(BrowserSessionModel model) {
        this.model = model;
    }

    public Region createSimple() {
        var multi = new MultiContentComp(Map.<Comp<?>, ObservableValue<Boolean>>of(
                Comp.of(() -> createTabPane()),
                Bindings.isNotEmpty(model.getSessionEntries()),
                new BrowserWelcomeComp(model).apply(struc -> StackPane.setAlignment(struc.get(), Pos.CENTER_LEFT)),
                Bindings.createBooleanBinding(
                        () -> {
                            return model.getSessionEntries().size() == 0;
                        },
                        model.getSessionEntries())));
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

        var map = new HashMap<BrowserSessionTab<?>, Tab>();

        // Restore state
        model.getSessionEntries().forEach(v -> {
            var t = createTab(tabs, v);
            map.put(v, t);
            tabs.getTabs().add(t);
        });
        tabs.getSelectionModel()
                .select(model.getSessionEntries().indexOf(model.getSelectedEntry().getValue()));

        // Used for ignoring changes by the tabpane when new tabs are added. We want to perform the selections manually!
        var modifying = new SimpleBooleanProperty();

        // Handle selection from platform
        tabs.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (modifying.get()) {
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
            });
        });

        model.getSessionEntries().addListener((ListChangeListener<? super BrowserSessionTab>) c -> {
            while (c.next()) {
                for (var r : c.getRemoved()) {
                    PlatformThread.runLaterIfNeeded(() -> {
                        try (var b = new BooleanScope(modifying).start()) {
                            var t = map.remove(r);
                            tabs.getTabs().remove(t);
                        }
                    });
                }

                for (var a : c.getAddedSubList()) {
                    PlatformThread.runLaterIfNeeded(() -> {
                        try (var b = new BooleanScope(modifying).start()) {
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
        return tabs;
    }

    private Tab createTab(TabPane tabs, BrowserSessionTab<?> model) {
        var tab = new Tab();

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

        tab.setContent(model.comp().createRegion());

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
                    var color = DataStorage.get()
                            .getRootForEntry(model.getEntry().get())
                            .getColor();
                    if (color != null) {
                        c.getStyleClass().add(color.getId());
                    }
                    new TooltipAugment<>(new SimpleStringProperty(model.getTooltip())).augment(c);
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
