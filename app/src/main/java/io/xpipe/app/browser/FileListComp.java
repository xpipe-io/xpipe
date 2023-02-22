/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import io.xpipe.app.comp.base.LazyTextFieldComp;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.util.Containers;
import io.xpipe.app.util.HumanReadableFormat;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.store.FileSystem;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.stream.Stream;

import static io.xpipe.app.util.HumanReadableFormat.byteCount;
import static javafx.scene.control.TableColumn.SortType.ASCENDING;

final class FileListComp extends AnchorPane {

    private static final PseudoClass HIDDEN = PseudoClass.getPseudoClass("hidden");
    private static final PseudoClass FOLDER = PseudoClass.getPseudoClass("folder");
    private static final PseudoClass DRAG = PseudoClass.getPseudoClass("drag");
    private static final String UNKNOWN = "unknown";

    private final FileListModel fileList;

    public FileListComp(FileListModel fileList) {
        this.fileList = fileList;
        TableView<FileSystem.FileEntry> table = createTable();
        SimpleChangeListener.apply(table.comparatorProperty(), (newValue) -> {
            fileList.setComparator(newValue);
        });

        getChildren().setAll(table);
        getStyleClass().addAll("table-directory-view");
        Containers.setAnchors(table, Insets.EMPTY);
    }

    @SuppressWarnings("unchecked")
    private TableView<FileSystem.FileEntry> createTable() {
        var editing = new SimpleObjectProperty<String>();
        var filenameCol = new TableColumn<FileSystem.FileEntry, String>("Name");
        filenameCol.setCellValueFactory(param -> new SimpleStringProperty(
                param.getValue() != null
                        ? FileNames.getFileName(param.getValue().getPath())
                        : null));
        filenameCol.setComparator(Comparator.comparing(String::toLowerCase));
        filenameCol.setSortType(ASCENDING);
        filenameCol.setCellFactory(col -> new FilenameCell(editing));

        var sizeCol = new TableColumn<FileSystem.FileEntry, Number>("Size");
        sizeCol.setCellValueFactory(
                param -> new SimpleLongProperty(param.getValue().getSize()));
        sizeCol.setCellFactory(col -> new FileSizeCell());

        var mtimeCol = new TableColumn<FileSystem.FileEntry, Instant>("Modified");
        mtimeCol.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getDate()));
        mtimeCol.setCellFactory(col -> new FileTimeCell());
        mtimeCol.getStyleClass().add(Tweaks.ALIGN_RIGHT);

        // ~

        var table = new TableView<FileSystem.FileEntry>();
        table.getStyleClass().add(Styles.STRIPED);
        table.getColumns().setAll(filenameCol, sizeCol, mtimeCol);
        table.getSortOrder().add(filenameCol);
        table.setSortPolicy(param -> true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        filenameCol.minWidthProperty().bind(table.widthProperty().multiply(0.5));

        table.setOnKeyPressed(event -> {
            if (event.isControlDown()
                    && event.getCode().equals(KeyCode.C)
                    && table.getSelectionModel().getSelectedItems().size() > 0) {
                BrowserClipboard.startCopy(table.getSelectionModel().getSelectedItems());
                event.consume();
            }

            if (event.isControlDown() && event.getCode().equals(KeyCode.V)) {
                var clipboard = BrowserClipboard.retrieveCopy();
                if (clipboard != null) {
                    var files = clipboard.getEntries();
                    var target = fileList.getModel().getCurrentDirectory();
                    fileList.getModel().dropFilesIntoAsync(target, files, true);
                    event.consume();
                }
            }
        });

        table.setRowFactory(param -> {
            TableRow<FileSystem.FileEntry> row = new TableRow<>();

            row.addEventHandler(MouseEvent.MOUSE_CLICKED, t -> {
                t.consume();
                if (row.isEmpty()) {
                    return;
                }

                var cm = new FileContextMenu(
                        fileList.getModel(),
                        row.getItem(),
                        editing);
                if (t.getButton() == MouseButton.SECONDARY) {
                    cm.show(row, t.getScreenX(), t.getScreenY());
                }
            });

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    fileList.onClick(row.getItem());
                }
            });

            row.setOnDragOver(event -> {
                if (row.equals(event.getGestureSource())) {
                    return;
                }

                row.pseudoClassStateChanged(DRAG, true);
                event.acceptTransferModes(TransferMode.ANY);
                event.consume();
            });

            row.setOnDragDetected(event -> {
                if (row.isEmpty()) {
                    return;
                }

                var url = AppResources.getResourceURL(AppResources.XPIPE_MODULE, "img/file_drag_icon.png")
                        .orElseThrow();
                var image = new Image(url.toString(), 80, 80, true, false);

                var selected = table.getSelectionModel().getSelectedItems();
                Dragboard db = row.startDragAndDrop(TransferMode.COPY);
                db.setContent(BrowserClipboard.startDrag(selected));
                db.setDragView(image, 30, 60);
                event.setDragDetect(true);
                event.consume();
            });

            row.setOnDragExited(event -> {
                row.pseudoClassStateChanged(DRAG, false);
                event.consume();

                if (event.getGestureSource() == null && event.getDragboard().hasFiles()) {
                    row.pseudoClassStateChanged(DRAG, false);
                    event.consume();
                }

                //                if (event.getGestureSource() != null) {
                //                    try {
                //                        var f = Files.createTempFile(null, null);
                //                        var cc = new ClipboardContent();
                //                        cc.putFiles(List.of(f.toFile()));
                //                        Dragboard db = row.startDragAndDrop(TransferMode.COPY);
                //                        db.setContent(cc);
                //                    } catch (IOException e) {
                //                        throw new RuntimeException(e);
                //                    }
                //                }
            });
            //
            //            row.setEventDispatcher((event, chain) -> {
            //                if (event.getEventType().getName().equals("MOUSE_DRAGGED")) {
            //                    MouseEvent drag = (MouseEvent) event;
            //
            //                    if (drag.isDragDetect()) {
            //                        return chain.dispatchEvent(event);
            //                    }
            //
            //                    Rectangle area = new Rectangle(
            //                            App.getApp().getStage().getX(),
            //                            App.getApp().getStage().getY(),
            //                            App.getApp().getStage().getWidth(),
            //                            App.getApp().getStage().getHeight()
            //                    );
            //                    if (!area.intersects(drag.getScreenX(), drag.getScreenY(), 20, 20)) {
            //                        System.out.println("->Drag down");
            //                        drag.setDragDetect(true);
            //                    }
            //                }
            //
            //                return chain.dispatchEvent(event);
            //            });

            row.setOnDragDropped(event -> {
                // Accept drops from outside the app window
                if (event.getGestureSource() == null && event.getDragboard().hasFiles()) {
                    event.setDropCompleted(true);
                    Dragboard db = event.getDragboard();
                    var list = db.getFiles().stream().map(File::toPath).toList();
                    var target = row.getItem() != null
                            ? row.getItem()
                            : fileList.getModel().getCurrentDirectory();
                    fileList.getModel().dropLocalFilesIntoAsync(target, list);
                }

                // Accept drops from inside the app window
                if (event.getGestureSource() != null) {
                    event.setDropCompleted(true);
                    var files = BrowserClipboard.retrieveDrag(event.getDragboard()).getEntries();
                    var target = row.getItem() != null
                            ? row.getItem()
                            : fileList.getModel().getCurrentDirectory();
                    fileList.getModel().dropFilesIntoAsync(target, files, false);
                }

                event.consume();
            });

            return row;
        });

        fileList.getShown().addListener((observable, oldValue, newValue) -> {
            table.getItems().setAll(newValue);

            if (newValue.size() > 0) {
                table.scrollTo(0);
            }
        });

        return table;
    }

    ///////////////////////////////////////////////////////////////////////////

    private class FilenameCell extends TableCell<FileSystem.FileEntry, String> {

        private final StringProperty img = new SimpleStringProperty();
        private final StringProperty text = new SimpleStringProperty();
        private final Node imageView = new PrettyImageComp(img, 24, 24).createRegion();
        private final StackPane textField =
                new LazyTextFieldComp(text).createStructure().get();
        private final ChangeListener<String> listener;

        public FilenameCell(Property<String> editing) {
            editing.addListener((observable, oldValue, newValue) -> {
                if (getTableRow().getItem() != null
                        && getTableRow().getItem().getPath().equals(newValue)) {
                    textField.requestFocus();
                }
            });

            listener = (observable, oldValue, newValue) -> {
                fileList.rename(oldValue, newValue);
                textField.getScene().getRoot().requestFocus();
                editing.setValue(null);
                updateItem(getItem(), isEmpty());
            };
        }

        @Override
        protected void updateItem(String fullPath, boolean empty) {
            super.updateItem(fullPath, empty);

            text.removeListener(listener);
            text.setValue(fullPath);

            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                img.set(null);
                setGraphic(null);
            } else {
                var isDirectory = getTableRow().getItem().isDirectory();
                var box = new HBox(imageView, textField);
                box.setSpacing(10);
                box.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(textField, Priority.ALWAYS);
                setGraphic(box);

                if (!isDirectory) {
                    img.set("file_drag_icon.png");
                } else {
                    img.set("folder_closed.svg");
                }

                pseudoClassStateChanged(FOLDER, isDirectory);

                var fileName = FileNames.getFileName(fullPath);
                var hidden = getTableRow().getItem().isHidden() || fileName.startsWith(".");
                getTableRow().pseudoClassStateChanged(HIDDEN, hidden);
                text.set(fileName);

                text.addListener(listener);
            }
        }
    }

    private class FileSizeCell extends TableCell<FileSystem.FileEntry, Number> {

        @Override
        protected void updateItem(Number fileSize, boolean empty) {
            super.updateItem(fileSize, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setText(null);
            } else {
                var path = getTableRow().getItem();
                if (path.isDirectory()) {
                    if (true) {
                        setText("");
                        return;
                    }

                    try (Stream<FileSystem.FileEntry> stream =
                            path.getFileSystem().listFiles(path.getPath())) {
                        setText(stream.count() + " items");
                    } catch (Exception e) {
                        setText(UNKNOWN);
                    }
                } else {
                    setText(byteCount(fileSize.longValue()));
                }
            }
        }
    }

    private static class FileTimeCell extends TableCell<FileSystem.FileEntry, Instant> {

        @Override
        protected void updateItem(Instant fileTime, boolean empty) {
            super.updateItem(fileTime, empty);
            if (empty) {
                setText(null);
            } else {
                setText(
                        fileTime != null
                                ? HumanReadableFormat.date(
                                        fileTime.atZone(ZoneId.systemDefault()).toLocalDateTime())
                                : UNKNOWN);
            }
        }
    }
}
