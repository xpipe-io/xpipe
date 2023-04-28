package io.xpipe.app.browser;

import io.xpipe.app.comp.storage.store.StoreEntryFlatMiniSection;
import io.xpipe.app.comp.storage.store.StoreEntryWrapper;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.DragPseudoClassAugment;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

final class BookmarkList extends SimpleComp {

    public static final Timer DROP_TIMER = new Timer("dnd", true);
    private Point2D lastOver = new Point2D(-1, -1);
    private TimerTask activeTask;

    private final FileBrowserModel model;

    BookmarkList(FileBrowserModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var map = StoreEntryFlatMiniSection.createMap();
        var list = new VBox();
        for (Map.Entry<StoreEntryWrapper, Region> e : map.entrySet()) {
            if (!(e.getKey().getEntry().getStore() instanceof ShellStore)) {
                continue;
            }

            var button = new Button(null, e.getValue());
            button.setOnAction(event -> {
                var fileSystem = ((ShellStore) e.getKey().getEntry().getStore());
                model.openFileSystem(fileSystem);
                event.consume();
            });
            button.prefWidthProperty().bind(list.widthProperty());
            DragPseudoClassAugment.create().augment(new SimpleCompStructure<>(button));

            button.addEventHandler(
                    DragEvent.DRAG_OVER,
                    mouseEvent -> handleHoverTimer(e.getKey().getEntry().getStore(), mouseEvent));
            button.addEventHandler(
                    DragEvent.DRAG_EXITED,
                    mouseEvent -> activeTask = null);

            list.getChildren().add(button);
        }
        list.setFillWidth(true);
        list.getStyleClass().add("bookmark-list");

        var sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        return sp;
    }

    private void handleHoverTimer(DataStore store, DragEvent event) {
        if (lastOver.getX() == event.getX() && lastOver.getY() == event.getY()) {
            return;
        }

        lastOver = (new Point2D(event.getX(), event.getY()));
        activeTask = new TimerTask() {
            @Override
            public void run() {
                if (activeTask != this) {
                    return;
                }

                Platform.runLater(() -> model.openExistingFileSystemIfPresent(store.asNeeded()));
            }
        };
        DROP_TIMER.schedule(activeTask, 500);
    }
}
