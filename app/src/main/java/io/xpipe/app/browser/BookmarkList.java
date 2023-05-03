package io.xpipe.app.browser;

import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.storage.store.StoreEntryFlatMiniSectionComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.DragPseudoClassAugment;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Region;

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
        var observableList = BindingsHelper.filteredContentBinding(StoreEntryFlatMiniSectionComp.ALL, e -> e.getEntry().getStore() instanceof ShellStore);
        var list = new ListBoxViewComp<>(observableList, observableList, e -> {
            return Comp.of(() -> {
                var button = new Button(null, e.createRegion());
                button.setOnAction(event -> {
                    var fileSystem = ((ShellStore) e.getEntry().getStore());
                    model.openFileSystem(fileSystem);
                    event.consume();
                });
                GrowAugment.create(true, false).augment(new SimpleCompStructure<>(button));
                DragPseudoClassAugment.create().augment(new SimpleCompStructure<>(button));

                button.addEventHandler(
                        DragEvent.DRAG_OVER,
                        mouseEvent -> handleHoverTimer(e.getEntry().getStore(), mouseEvent));
                button.addEventHandler(
                        DragEvent.DRAG_EXITED,
                        mouseEvent -> activeTask = null);

                return button;
            });
        }).styleClass("bookmark-list").createRegion();
        return list;
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
