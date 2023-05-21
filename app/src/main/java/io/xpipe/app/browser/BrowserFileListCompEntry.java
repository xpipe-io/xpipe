package io.xpipe.app.browser;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import lombok.Getter;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

@Getter
public class BrowserFileListCompEntry {

    public static final Timer DROP_TIMER = new Timer("dnd", true);

    private final Node row;
    private final BrowserEntry item;
    private final BrowserFileListModel model;

    private Point2D lastOver = new Point2D(-1, -1);
    private TimerTask activeTask;

    public BrowserFileListCompEntry(Node row, BrowserEntry item, BrowserFileListModel model) {
        this.row = row;
        this.item = item;
        this.model = model;
    }

    @SuppressWarnings("unchecked")
    public void onMouseClick(MouseEvent t) {
        if (item == null) {
            model.getSelection().clear();
            return;
        }

        if (t.getClickCount() == 2 && t.getButton() == MouseButton.PRIMARY) {
            model.onDoubleClick(item);
            t.consume();
            return;
        }

        if (isSynthetic()) {
            return;
        }

        if (t.getButton() == MouseButton.PRIMARY && t.isShiftDown()) {
            var tv = ((TableView<BrowserEntry>) row.getParent().getParent().getParent().getParent());
            var all = tv.getItems();
            var min = tv.getSelectionModel().getSelectedIndices().stream().mapToInt(value -> value).min().orElse(1);
            var max = tv.getSelectionModel().getSelectedIndices().stream().mapToInt(value -> value).max().orElse(all.size() - 1);
            var end = tv.getSelectionModel().getFocusedIndex();
            var start = end > min ? min : max;
            tv.getSelectionModel().selectRange(Math.min(start, end), Math.max(start, end) + 1);
            t.consume();
            return;
        }
    }

    public boolean isSynthetic() {
        return item != null && item.getRawFileEntry().equals(model.getFileSystemModel().getCurrentParentDirectory());
    }

    private boolean acceptsDrop(DragEvent event) {
        // Accept drops from outside the app window
        if (event.getGestureSource() == null) {
            return true;
        }

        if (BrowserClipboard.currentDragClipboard == null) {
            return false;
        }

        if (model.getFileSystemModel().getCurrentDirectory() == null) {
            return false;
        }

        // Prevent drag and drops of files into the current directory
        if (BrowserClipboard.currentDragClipboard
                .getBaseDirectory().getPath()
                .equals(model.getFileSystemModel().getCurrentDirectory().getPath()) && (item == null || !item.getRawFileEntry().isDirectory())) {
            return false;
        }

        // Prevent dropping items onto themselves
        if (item != null && BrowserClipboard.currentDragClipboard.getEntries().contains(item)) {
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
            var target = item != null && item.getRawFileEntry().isDirectory()
                    ? item.getRawFileEntry()
                    : model.getFileSystemModel().getCurrentDirectory();
            model.getFileSystemModel().dropLocalFilesIntoAsync(target, list);
            event.setDropCompleted(true);
            event.consume();
        }

        // Accept drops from inside the app window
        if (event.getGestureSource() != null) {
            var files = BrowserClipboard.retrieveDrag(event.getDragboard()).getEntries();
            var target = item != null && item.getRawFileEntry().isDirectory()
                    ? item.getRawFileEntry()
                    : model.getFileSystemModel().getCurrentDirectory();
            model.getFileSystemModel().dropFilesIntoAsync(target, files, false);
            event.setDropCompleted(true);
            event.consume();
        }
    }

    public void onDragExited(DragEvent event) {
        if (item != null && item.getRawFileEntry().isDirectory()) {
            model.getDraggedOverDirectory().setValue(null);
        } else {
            model.getDraggedOverEmpty().setValue(false);
        }
        event.consume();
    }

    public void startDrag(MouseEvent event) {
        if (item == null) {
            row.startFullDrag();
            return;
        }

        if (isSynthetic()) {
            return;
        }

        var selected = model.getSelectedRaw();
        Dragboard db = row.startDragAndDrop(TransferMode.COPY);
        db.setContent(BrowserClipboard.startDrag(model.getFileSystemModel().getCurrentDirectory(), selected));

        Image image = BrowserSelectionListComp.snapshot(selected);
        db.setDragView(image, -20, 15);

        event.setDragDetect(true);
        event.consume();
    }

    private void acceptDrag(DragEvent event) {
        model.getDraggedOverEmpty().setValue(item == null || !item.getRawFileEntry().isDirectory());
        model.getDraggedOverDirectory().setValue(item);
        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
    }

    private void handleHoverTimer(DragEvent event) {
        if (item == null || !item.getRawFileEntry().isDirectory()) {
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

                model.getFileSystemModel().cd(item.getRawFileEntry().getPath());
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

    @SuppressWarnings("unchecked")
    public void onMouseDragEntered(MouseDragEvent event) {
        event.consume();

        if (model.getFileSystemModel().getCurrentDirectory() == null) {
            return;
        }

        if (item == null || item.isSynthetic()) {
            return;
        }

        var tv = ((TableView<BrowserEntry>) row.getParent().getParent().getParent().getParent());
        tv.getSelectionModel().select(item);
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
