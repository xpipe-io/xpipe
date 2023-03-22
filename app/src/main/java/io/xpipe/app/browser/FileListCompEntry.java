package io.xpipe.app.browser;

import io.xpipe.app.core.AppResources;
import io.xpipe.core.store.FileSystem;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import lombok.Getter;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

@Getter
public class FileListCompEntry {

    public static final Timer DROP_TIMER = new Timer("dnd", true);

    private final Node row;
    private final FileSystem.FileEntry item;
    private final FileListModel model;

    private Point2D lastOver = new Point2D(-1, -1);
    private TimerTask activeTask;
    private FileContextMenu currentContextMenu;

    public FileListCompEntry(Node row, FileSystem.FileEntry item, FileListModel model) {
        this.row = row;
        this.item = item;
        this.model = model;
    }

    public void onMouseClick(MouseEvent t) {
        t.consume();
        if (item == null || isSynthetic()) {
            return;
        }

        if (currentContextMenu != null) {
            currentContextMenu.hide();
            currentContextMenu = null;
        }

        if (t.getButton() == MouseButton.SECONDARY) {
            var cm = new FileContextMenu(model.getFileSystemModel(), item, model.getEditing());
            cm.show(row, t.getScreenX(), t.getScreenY());
            currentContextMenu = cm;
        }
    }

    public boolean isSynthetic() {
        return item != null && item.equals(model.getFileSystemModel().getCurrentParentDirectory());
    }

    private boolean acceptsDrop(DragEvent event) {
        // Accept drops from outside the app window
        if (event.getGestureSource() == null) {
            return true;
        }

        if (FileBrowserClipboard.currentDragClipboard == null) {
            return false;
        }

        // Prevent drag and drops of files into the current directory
        if (FileBrowserClipboard.currentDragClipboard
                .getBaseDirectory().getPath()
                .equals(model.getFileSystemModel().getCurrentDirectory().getPath()) && (item == null || !item.isDirectory())) {
            return false;
        }

        // Prevent dropping items onto themselves
        if (item != null && FileBrowserClipboard.currentDragClipboard.getEntries().contains(item)) {
            return false;
        }

        return true;
    }

    public void onDragDrop(DragEvent event) {
        model.getDraggedOverEmpty().setValue(false);
        model.getDraggedOverDirectory().setValue(null);

        // Accept drops from outside the app window
        if (event.getGestureSource() == null && event.getDragboard().hasFiles()) {
            Dragboard db = event.getDragboard();
            var list = db.getFiles().stream().map(File::toPath).toList();
            var target = item != null && item.isDirectory()
                    ? item
                    : model.getFileSystemModel().getCurrentDirectory();
            model.getFileSystemModel().dropLocalFilesIntoAsync(target, list);
            event.setDropCompleted(true);
            event.consume();
        }

        // Accept drops from inside the app window
        if (event.getGestureSource() != null) {
            var files = FileBrowserClipboard.retrieveDrag(event.getDragboard()).getEntries();
            var target = item != null
                    ? item
                    : model.getFileSystemModel().getCurrentDirectory();
            model.getFileSystemModel().dropFilesIntoAsync(target, files, false);
            event.setDropCompleted(true);
            event.consume();
        }
    }

    public void onDragExited(DragEvent event) {
        if (item != null && item.isDirectory()) {
            model.getDraggedOverDirectory().setValue(null);
        } else {
            model.getDraggedOverEmpty().setValue(false);
        }
        event.consume();
    }

    public void startDrag(MouseEvent event) {
        if (item == null) {
            return;
        }

        if (isSynthetic()) {
            return;
        }

        var url = AppResources.getResourceURL(AppResources.XPIPE_MODULE, "img/file_drag_icon.png")
                .orElseThrow();
        var image = new Image(url.toString(), 80, 80, true, false);
        var selected = model.getSelected();
        Dragboard db = row.startDragAndDrop(TransferMode.COPY);
        db.setContent(FileBrowserClipboard.startDrag(model.getFileSystemModel().getCurrentDirectory(), selected));
        db.setDragView(image, 30, 60);
        event.setDragDetect(true);
        event.consume();
    }

    private void acceptDrag(DragEvent event) {
        if (item == null || !item.isDirectory()) {
            model.getDraggedOverEmpty().setValue(true);
        } else {
            model.getDraggedOverDirectory().setValue(item);
        }
        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
    }

    private void handleHoverTimer(DragEvent event) {
        if (item == null || !item.isDirectory()) {
            return;
        }

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

                if (item != model.getDraggedOverDirectory().getValue()) {
                    return;
                }

                model.getFileSystemModel().cd(item.getPath());
            }
        };
        DROP_TIMER.schedule(activeTask, 1000);
    }

    public void onDragEntered(DragEvent event) {
        event.consume();
        if (!acceptsDrop(event)) {
            return;
        }

        acceptDrag(event);
    }

    public void onDragOver(DragEvent event) {
        event.consume();
        if (!acceptsDrop(event)) {
            return;
        }

        acceptDrag(event);
        handleHoverTimer(event);
    }
}
