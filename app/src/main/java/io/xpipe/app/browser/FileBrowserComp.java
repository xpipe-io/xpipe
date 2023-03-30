package io.xpipe.app.browser;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.BusyProperty;
import io.xpipe.core.store.FileSystem;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.*;

import java.util.HashMap;

import static atlantafx.base.theme.Styles.DENSE;
import static atlantafx.base.theme.Styles.toggleStyleClass;
import static javafx.scene.control.TabPane.TabClosingPolicy.ALL_TABS;

public class FileBrowserComp extends SimpleComp {

    private final FileBrowserModel model;

    public FileBrowserComp(FileBrowserModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var bookmarksList = new BookmarkList(model).createRegion();
        var localDownloadStage = new LocalFileTransferComp(model.getLocalTransfersStage()).createRegion();
        var vertical = new VBox(bookmarksList, localDownloadStage);
        vertical.setFillWidth(true);

        var splitPane = new SplitPane(vertical, createTabs());
        splitPane
                .widthProperty()
                .addListener(
                        // set sidebar width in pixels depending on split pane width
                        (obs, old, val) -> splitPane.setDividerPosition(0, 230 / splitPane.getWidth()));

        return addBottomBar(splitPane);
    }

    private Region addBottomBar(Region r) {
        if (model.getMode().equals(FileBrowserModel.Mode.BROWSER)) {
            return r;
        }

        var selectedLabel = new Label("Selected: ");
        selectedLabel.setAlignment(Pos.CENTER);
        var selected = new HBox();
        selected.setAlignment(Pos.CENTER_LEFT);
        selected.setSpacing(10);
        model.getSelectedFiles().addListener((ListChangeListener<? super FileSystem.FileEntry>) c -> {
            selected.getChildren().setAll(c.getList().stream().map(s -> {
                var field = new TextField(s.getPath());
                field.setEditable(false);
                field.setPrefWidth(400);
                return field;
            }).toList());
        });
        var spacer = new Spacer(Orientation.HORIZONTAL);
        var button = new Button("Select");
        button.setOnAction(event -> model.finishChooser());
        button.setDefaultButton(true);
        var bottomBar = new HBox(selectedLabel, selected, spacer, button);
        HBox.setHgrow(selected, Priority.ALWAYS);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setPadding(new Insets(15));
        bottomBar.getStyleClass().add("chooser-bar");

        var layout = new VBox(r, bottomBar);
        VBox.setVgrow(r, Priority.ALWAYS);
        return layout;
    }

    private Node createTabs() {
        var stack = new StackPane();
        var tabs = createTabPane();
        stack.getChildren().add(tabs);

        var map = new HashMap<OpenFileSystemModel, Tab>();

        // Restore state
        model.getOpenFileSystems().forEach(v -> {
            var t = createTab(tabs, v);
            map.put(v, t);
            tabs.getTabs().add(t);
        });
        tabs.getSelectionModel().select(model.getOpenFileSystems().indexOf(model.getSelected().getValue()));

        // Used for ignoring changes by the tabpane when new tabs are added. We want to perform the selections manually!
        var modifying = new SimpleBooleanProperty();

        // Handle selection from platform
        tabs.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (modifying.get()) {
                return;
            }

            if (newValue.intValue() == -1) {
                model.getSelected().setValue(null);
                return;
            }

            model.getSelected().setValue(model.getOpenFileSystems().get(newValue.intValue()));
        });

        // Handle selection from model
        model.getSelected().addListener((observable, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                tabs.getSelectionModel().select(model.getOpenFileSystems().indexOf(newValue));
            });
        });

        model.getOpenFileSystems().addListener((ListChangeListener<? super OpenFileSystemModel>) c -> {
            while (c.next()) {
                for (var r : c.getRemoved()) {
                    PlatformThread.runLaterIfNeeded(() -> {
                        try (var b = new BusyProperty(modifying)) {
                            var t = map.remove(r);
                            tabs.getTabs().remove(t);
                        }
                    });
                }

                for (var a : c.getAddedSubList()) {
                    PlatformThread.runLaterIfNeeded(() -> {
                        try (var b = new BusyProperty(modifying)) {
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

                    model.closeFileSystem(source.getKey());
                }
            }
        });

        stack.getStyleClass().add("browser");
        return stack;
    }

    private TabPane createTabPane() {
        var tabs = new TabPane();
        tabs.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        tabs.setTabMinWidth(Region.USE_COMPUTED_SIZE);

        if (!model.getMode().equals(FileBrowserModel.Mode.BROWSER)) {
            tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            tabs.getStyleClass().add("singular");
        } else {
            tabs.setTabClosingPolicy(ALL_TABS);
            Styles.toggleStyleClass(tabs, TabPane.STYLE_CLASS_FLOATING);
            toggleStyleClass(tabs, DENSE);
        }

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
                                    .getStoreEntry(model.getStore().getValue())
                                    .getName()
                            : null;
                },
                PlatformThread.sync(model.getStore()));
        var image = Bindings.createStringBinding(
                () -> {
                    return model.getStore().getValue() != null
                            ? DataStorage.get()
                                    .getStoreEntry(model.getStore().getValue())
                                    .getProvider()
                                    .getDisplayIconFileName(model.getStore().getValue())
                            : null;
                },
                model.getStore());
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

        if (!this.model.getMode().equals(FileBrowserModel.Mode.BROWSER)) {
            label.setManaged(false);
            label.setVisible(false);
        }

        tab.setContent(new OpenFileSystemComp(model).createSimple());
        return tab;
    }
}
