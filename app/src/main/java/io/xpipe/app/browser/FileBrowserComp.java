package io.xpipe.app.browser;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.theme.Styles;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStorage;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.HashMap;

import static atlantafx.base.theme.Styles.DENSE;
import static atlantafx.base.theme.Styles.toggleStyleClass;
import static javafx.scene.control.TabPane.TabClosingPolicy.ALL_TABS;

public class FileBrowserComp extends SimpleComp {

    private static final double TAB_MIN_HEIGHT = 60;

    private final FileBrowserModel model;

    public FileBrowserComp(FileBrowserModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var bookmarksList = new BookmarkList(model).createRegion();
        var splitPane = new SplitPane(bookmarksList, createTabs());
        splitPane
                .widthProperty()
                .addListener(
                        // set sidebar width in pixels depending on split pane width
                        (obs, old, val) -> splitPane.setDividerPosition(0, 230 / splitPane.getWidth()));

        return splitPane;
    }

    private Node createTabs() {
        var stack = new StackPane();
        var tabs = createTabPane();
        stack.getChildren().add(tabs);

        var map = new HashMap<OpenFileSystemModel, Tab>();

        tabs.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == -1) {
                model.getSelected().setValue(null);
                return;
            }

            model.getSelected().setValue(model.getOpenFileSystems().get(newValue.intValue()));
        });

        model.getOpenFileSystems().forEach(v -> {
            var t = createTab(tabs, v);
            map.put(v, t);
            tabs.getTabs().add(t);
        });
        if (model.getOpenFileSystems().size() > 0) {
            tabs.getSelectionModel().select(0);
        }

        model.getOpenFileSystems().addListener((ListChangeListener<? super OpenFileSystemModel>) c -> {
            PlatformThread.runLaterIfNeededBlocking(() -> {
                while (c.next()) {
                    for (var r : c.getRemoved()) {
                        var t = map.remove(r);
                        tabs.getTabs().remove(t);
                    }

                    for (var a : c.getAddedSubList()) {
                        var t = createTab(tabs, a);
                        map.put(a, t);
                        tabs.getTabs().add(t);
                    }
                }
            });
        });

        model.getSelected().addListener((observable, oldValue, newValue) -> {
            tabs.getSelectionModel().select(model.getOpenFileSystems().indexOf(newValue));
        });

        tabs.getTabs().addListener((ListChangeListener<? super Tab>) c -> {
            while (c.next()) {
                for (var r : c.getRemoved()) {
                    var source = map.entrySet().stream()
                            .filter(openFileSystemModelTabEntry ->
                                    openFileSystemModelTabEntry.getValue().equals(r))
                            .findAny()
                            .orElseThrow();
                    model.closeFileSystem(source.getKey());
                }
            }
        });

        stack.getStyleClass().add("browser");
        return stack;
    }

    private Node createSingular() {
        var stack =
                new StackPane(new OpenFileSystemComp(model.getOpenFileSystems().get(0)).createSimple());
        return stack;
    }

    private TabPane createTabPane() {
        var tabs = new TabPane();
        tabs.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        tabs.setTabClosingPolicy(ALL_TABS);
        Styles.toggleStyleClass(tabs, TabPane.STYLE_CLASS_FLOATING);
        // tabs.setStyle("-fx-open-tab-animation:none;-fx-close-tab-animation:none;");
        toggleStyleClass(tabs, DENSE);
        tabs.setMinHeight(TAB_MIN_HEIGHT);
        tabs.setTabMinWidth(Region.USE_COMPUTED_SIZE);

        return tabs;
    }

    private Tab createTab(TabPane tabs, OpenFileSystemModel model) {
        var tab = new Tab();

        var ring = new RingProgressIndicator(0, false);
        ring.setMinSize(14, 14);
        ring.setPrefSize(14, 14);
        ring.setMaxSize(14, 14);
        ring.progressProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> model.getBusy().get() ? -1d : 0, PlatformThread.sync(model.getBusy())));

        var name = Bindings.createStringBinding(
                () -> {
                    return model.getStore().getValue() != null
                            ? DataStorage.get()
                                    .getEntryByStore(model.getStore().getValue())
                                    .orElseThrow()
                                    .getName()
                            : null;
                },
                PlatformThread.sync(model.getStore()));
        var image = Bindings.createStringBinding(
                () -> {
                    return model.getStore().getValue() != null
                            ? DataStorage.get()
                                    .getEntryByStore(model.getStore().getValue())
                                    .orElseThrow()
                                    .getProvider()
                                    .getDisplayIconFileName()
                            : null;
                },
                PlatformThread.sync(model.getStore()));
        var logo = new PrettyImageComp(image, 20, 20).createRegion();

        var label = new Label();
        label.textProperty().bind(name);
        label.addEventHandler(DragEvent.DRAG_ENTERED, new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent mouseEvent) {
                tabs.getSelectionModel().select(tab);
            }
        });

        label.graphicProperty()
                .bind(Bindings.createObjectBinding(
                        () -> {
                            return model.getBusy().get() ? ring : logo;
                        },
                        PlatformThread.sync(model.getBusy())));

        tab.setGraphic(label);
        tab.setContent(new OpenFileSystemComp(model).createSimple());
        return tab;
    }
}
