package io.xpipe.app.browser.file;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FileKind;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.input.*;

import lombok.Getter;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;

@Getter
public class BrowserFileListCompEntry {

    private final TableView<BrowserEntry> tv;
    private final Node row;
    private final BrowserEntry item;
    private final BrowserFileListModel model;

    private Point2D lastOver = new Point2D(-1, -1);
    private Runnable activeTask;
    private ContextMenu lastContextMenu;

    public BrowserFileListCompEntry(
            TableView<BrowserEntry> tv, Node row, BrowserEntry item, BrowserFileListModel model) {
        this.tv = tv;
        this.row = row;
        this.item = item;
        this.model = model;
    }

    public void onMouseClick(MouseEvent t) {
        if (lastContextMenu != null) {
            lastContextMenu.hide();
            lastContextMenu = null;
        }

        if (showContextMenu(t)) {
            var cm = new BrowserContextMenu(model.getFileSystemModel(), item, false);
            cm.show(row, t.getScreenX(), t.getScreenY());
            lastContextMenu = cm;
            t.consume();
            return;
        }

        if (t.getButton() == MouseButton.BACK) {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(model.getFileSystemModel().getBusy(), () -> {
                    model.getFileSystemModel().backSync(1);
                });
            });
            t.consume();
            return;
        }

        if (t.getButton() == MouseButton.FORWARD) {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(model.getFileSystemModel().getBusy(), () -> {
                    model.getFileSystemModel().forthSync(1);
                });
            });
            t.consume();
            return;
        }

        if (item == null) {
            // Only clear for normal clicks
            if (t.isStillSincePress()) {
                model.getSelection().clear();
                if (tv != null) {
                    tv.requestFocus();
                }
            }
            t.consume();
            return;
        }

        row.requestFocus();
        if (t.getClickCount() == 2 && t.getButton() == MouseButton.PRIMARY) {
            model.onDoubleClick(item);
            t.consume();
        }

        t.consume();
    }

    private boolean showContextMenu(MouseEvent event) {
        if (item == null) {
            return event.getButton() == MouseButton.SECONDARY;
        }

        if (item.getRawFileEntry().resolved().getKind() == FileKind.DIRECTORY) {
            return event.getButton() == MouseButton.SECONDARY;
        }

        if (item.getRawFileEntry().resolved().getKind() != FileKind.DIRECTORY) {
            return event.getButton() == MouseButton.SECONDARY
                    || !AppPrefs.get().editFilesWithDoubleClick().get()
                            && event.getButton() == MouseButton.PRIMARY
                            && event.getClickCount() == 2;
        }

        return false;
    }

    public void onMouseShiftClick(MouseEvent t) {
        if (t.getButton() != MouseButton.PRIMARY) {
            return;
        }

        var all = tv.getItems();
        var index = item != null ? all.indexOf(item) : all.size() - 1;
        var min = Math.min(
                index,
                tv.getSelectionModel().getSelectedIndices().stream()
                        .mapToInt(value -> value)
                        .min()
                        .orElse(1));
        var max = Math.max(
                index,
                tv.getSelectionModel().getSelectedIndices().stream()
                        .mapToInt(value -> value)
                        .max()
                        .orElse(all.indexOf(item)));

        var toSelect = new ArrayList<BrowserEntry>();
        for (int i = min; i <= max; i++) {
            if (!model.getSelection().contains(model.getShown().getValue().get(i))) {
                toSelect.add(model.getShown().getValue().get(i));
            }
        }
        model.getSelection().addAll(toSelect);
        t.consume();
    }

    private boolean acceptsDrop(DragEvent event) {
        // Accept drops from outside the app window
        if (event.getGestureSource() == null) {
            return true;
        }

        BrowserClipboard.Instance cb = BrowserClipboard.currentDragClipboard;
        if (cb == null) {
            return false;
        }

        if (model.getFileSystemModel().getCurrentDirectory() == null) {
            return false;
        }

        if (!Objects.equals(
                model.getFileSystemModel().getFileSystem(),
                cb.getEntries().getFirst().getRawFileEntry().getFileSystem())) {
            return true;
        }

        // Prevent drag and drops of files into the current directory
        if (cb.getBaseDirectory() != null
                && cb.getBaseDirectory()
                        .getPath()
                        .equals(model.getFileSystemModel().getCurrentDirectory().getPath())
                && (item == null || item.getRawFileEntry().getKind() != FileKind.DIRECTORY)) {
            return false;
        }

        // Prevent dropping items onto themselves
        if (item != null && cb.getEntries().contains(item)) {
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
            var target = item != null && item.getRawFileEntry().getKind() == FileKind.DIRECTORY
                    ? item.getRawFileEntry()
                    : model.getFileSystemModel().getCurrentDirectory();
            model.getFileSystemModel().dropLocalFilesIntoAsync(target, list);
            event.setDropCompleted(true);
            event.consume();
        }

        // Accept drops from inside the app window
        if (event.getGestureSource() != null) {
            var db = BrowserClipboard.retrieveDrag(event.getDragboard());
            if (db == null) {
                return;
            }

            var files = db.getEntries();
            var target = item != null && item.getRawFileEntry().getKind() == FileKind.DIRECTORY
                    ? item.getRawFileEntry()
                    : model.getFileSystemModel().getCurrentDirectory();
            model.getFileSystemModel()
                    .dropFilesIntoAsync(
                            target,
                            files.stream()
                                    .map(browserEntry -> browserEntry.getRawFileEntry())
                                    .toList(),
                            db.getMode());
            event.setDropCompleted(true);
            event.consume();
        }
    }

    public void onDragExited(DragEvent event) {
        if (item != null && item.getRawFileEntry().getKind() == FileKind.DIRECTORY) {
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

        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        if (model.getFileSystemModel().getBrowserModel() instanceof BrowserFullSessionModel sessionModel) {
            sessionModel.getDraggingFiles().setValue(true);
        }
        var selected = model.getSelection();
        Dragboard db = row.startDragAndDrop(TransferMode.COPY);
        db.setContent(BrowserClipboard.startDrag(
                model.getFileSystemModel().getCurrentDirectory(),
                selected,
                event.isAltDown() ? BrowserFileTransferMode.MOVE : BrowserFileTransferMode.NORMAL));

        Image image = BrowserFileSelectionListComp.snapshot(selected);
        db.setDragView(image, -20, 15);

        event.setDragDetect(true);
        event.consume();
    }

    public void onDragDone(DragEvent event) {
        if (model.getFileSystemModel().getBrowserModel() instanceof BrowserFullSessionModel sessionModel) {
            sessionModel.getDraggingFiles().setValue(false);
            event.consume();
        }
    }

    private void acceptDrag(DragEvent event) {
        model.getDraggedOverEmpty()
                .setValue(item == null || item.getRawFileEntry().getKind() != FileKind.DIRECTORY);
        model.getDraggedOverDirectory().setValue(item);
        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
    }

    private void handleHoverTimer(DragEvent event) {
        if (item == null || item.getRawFileEntry().getKind() != FileKind.DIRECTORY) {
            return;
        }

        if (lastOver.getX() == event.getX() && lastOver.getY() == event.getY()) {
            return;
        }

        lastOver = (new Point2D(event.getX(), event.getY()));
        activeTask = new Runnable() {
            @Override
            public void run() {
                if (activeTask != this) {
                    return;
                }

                if (item != model.getDraggedOverDirectory().getValue()) {
                    return;
                }

                model.getFileSystemModel()
                        .cdAsync(item.getRawFileEntry().getPath().toString());
            }
        };
        GlobalTimer.delayAsync(activeTask, Duration.ofMillis(1200));
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

        if (item == null) {
            return;
        }

        var tv = ((TableView<BrowserEntry>)
                row.getParent().getParent().getParent().getParent());
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
