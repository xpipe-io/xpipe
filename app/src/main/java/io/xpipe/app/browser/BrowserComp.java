package io.xpipe.app.browser;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import io.xpipe.app.browser.icon.DirectoryType;
import io.xpipe.app.browser.icon.FileIconManager;
import io.xpipe.app.browser.icon.FileType;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.BusyProperty;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
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

public class BrowserComp extends SimpleComp {

    private final BrowserModel model;

    public BrowserComp(BrowserModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        FileType.loadDefinitions();
        DirectoryType.loadDefinitions();
        ThreadHelper.runAsync(() -> {
            FileIconManager.loadIfNecessary();
        });

        var bookmarksList = new BrowserBookmarkList(model).createRegion();
        VBox.setVgrow(bookmarksList, Priority.ALWAYS);
        var localDownloadStage = new BrowserTransferComp(model.getLocalTransfersStage())
                .hide(PlatformThread.sync(Bindings.createBooleanBinding(
                        () -> {
                            if (model.getOpenFileSystems().size() == 0) {
                                return true;
                            }

                            if (model.getMode().isChooser()) {
                                return true;
                            }

                            if (model.getSelected().getValue() != null) {
                                return model.getSelected().getValue().isLocal();
                            }

                            return false;
                        },
                        model.getOpenFileSystems(),
                        model.getSelected())))
                .createRegion();
        localDownloadStage.setPrefHeight(200);
        localDownloadStage.setMaxHeight(200);
        var vertical = new VBox(bookmarksList, localDownloadStage);
        vertical.setFillWidth(true);

        var splitPane = new SplitPane(vertical, createTabs());
        splitPane
                .widthProperty()
                .addListener(
                        // set sidebar width in pixels depending on split pane width
                        (obs, old, val) -> splitPane.setDividerPosition(0, 280 / splitPane.getWidth()));

        var r = addBottomBar(splitPane);
        r.getStyleClass().add("browser");
        // AppFont.small(r);
        return r;
    }

    private Region addBottomBar(Region r) {
        if (!model.getMode().isChooser()) {
            return r;
        }

        var selectedLabel = new Label("Selected: ");
        selectedLabel.setAlignment(Pos.CENTER);
        var selected = new HBox();
        selected.setAlignment(Pos.CENTER_LEFT);
        selected.setSpacing(10);
        model.getSelection().addListener((ListChangeListener<? super BrowserEntry>) c -> {
            PlatformThread.runLaterIfNeeded(() -> {
                selected.getChildren()
                        .setAll(c.getList().stream()
                                        .map(s -> {
                                            var field = new TextField(s.getRawFileEntry().getPath());
                                            field.setEditable(false);
                                            field.setPrefWidth(400);
                                            return field;
                                        })
                                        .toList());
            });
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
        tabs.getSelectionModel()
                .select(model.getOpenFileSystems().indexOf(model.getSelected().getValue()));

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
                var index = model.getOpenFileSystems().indexOf(newValue);
                if (index == -1 || index >= tabs.getTabs().size()) {
                    tabs.getSelectionModel().select(null);
                    return;
                }

                var tab = tabs.getTabs().get(index);
                tabs.getSelectionModel().select(tab);
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

                    model.closeFileSystemAsync(source.getKey());
                }
            }
        });
        return stack;
    }

    private TabPane createTabPane() {
        var tabs = new TabPane();
        tabs.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        tabs.setTabMinWidth(Region.USE_COMPUTED_SIZE);
        tabs.setTabClosingPolicy(ALL_TABS);
        Styles.toggleStyleClass(tabs, TabPane.STYLE_CLASS_FLOATING);
        toggleStyleClass(tabs, DENSE);
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

        var image = DataStoreProviders.byStore(model.getStore())
                .getDisplayIconFileName(model.getStore());
        var logo = new PrettyImageComp(new SimpleStringProperty(image), 20, 20).createRegion();

        var label = new Label(model.getName());
        label.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        label.addEventHandler(DragEvent.DRAG_ENTERED, new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent mouseEvent) {
                Platform.runLater(() -> tabs.getSelectionModel().select(tab));
            }
        });

        label.graphicProperty()
                .bind(Bindings.createObjectBinding(
                        () -> {
                            return model.getBusy().get() ? ring : logo;
                        },
                        PlatformThread.sync(model.getBusy())));

        tab.setGraphic(label);
        new FancyTooltipAugment<>(new SimpleStringProperty(model.getName())).augment(label);
        GrowAugment.create(true, false).augment(new SimpleCompStructure<>(label));
        tab.setContent(new OpenFileSystemComp(model).createSimple());
        tab.setText(model.getName());
        return tab;
    }
}
