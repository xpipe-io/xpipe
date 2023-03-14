/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import io.xpipe.app.comp.base.LazyTextFieldComp;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.util.Containers;
import io.xpipe.app.util.HumanReadableFormat;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.store.FileSystem;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

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
    private static final PseudoClass DRAG_OVER = PseudoClass.getPseudoClass("drag-over");
    private static final PseudoClass DRAG_INTO_CURRENT = PseudoClass.getPseudoClass("drag-into-current");
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
        var editing = new SimpleObjectProperty<FileSystem.FileEntry>();
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
        table.setPlaceholder(new Region());
        table.getStyleClass().add(Styles.STRIPED);
        table.getColumns().setAll(filenameCol, sizeCol, mtimeCol);
        table.getSortOrder().add(filenameCol);
        table.setSortPolicy(param -> true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        filenameCol.minWidthProperty().bind(table.widthProperty().multiply(0.5));

        if (fileList.getMode().equals(FileBrowserModel.Mode.SINGLE_FILE_CHOOSER) || fileList.getMode().equals(FileBrowserModel.Mode.DIRECTORY_CHOOSER)) {
            table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        } else {
            table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }

        table.getSelectionModel().getSelectedItems().addListener((ListChangeListener<? super FileSystem.FileEntry>) c -> {
            fileList.getSelected().setAll(c.getList());
            fileList.getFileSystemModel().getBrowserModel().getSelectedFiles().setAll(c.getList());
        });

        table.setOnKeyPressed(event -> {
            if (event.isControlDown()
                    && event.getCode().equals(KeyCode.C)
                    && table.getSelectionModel().getSelectedItems().size() > 0) {
                FileBrowserClipboard.startCopy(fileList.getFileSystemModel().getCurrentDirectory(), table.getSelectionModel().getSelectedItems());
                event.consume();
            }

            if (event.isControlDown() && event.getCode().equals(KeyCode.V)) {
                var clipboard = FileBrowserClipboard.retrieveCopy();
                if (clipboard != null) {
                    var files = clipboard.getEntries();
                    var target = fileList.getFileSystemModel().getCurrentDirectory();
                    fileList.getFileSystemModel().dropFilesIntoAsync(target, files, true);
                    event.consume();
                }
            }
        });

        var emptyEntry = new FileListEntry(table, null, fileList);
        table.setOnDragOver(event -> {
            emptyEntry.onDragOver(event);
        });
        table.setOnDragEntered(event -> {
            emptyEntry.onDragEntered(event);
        });
        table.setOnDragDetected(event -> {
            emptyEntry.startDrag(event);
        });
        table.setOnDragExited(event -> {
            emptyEntry.onDragExited(event);
        });
        table.setOnDragDropped(event -> {
            emptyEntry.onDragDrop(event);
        });

        table.setRowFactory(param -> {
            TableRow<FileSystem.FileEntry> row = new TableRow<>();
            var listEntry = Bindings.createObjectBinding(() -> new FileListEntry(row, row.getItem(), fileList), row.itemProperty());

            row.itemProperty().addListener((observable, oldValue, newValue) -> {
                row.pseudoClassStateChanged(DRAG, false);
                row.pseudoClassStateChanged(DRAG_OVER, false);
            });

            row.addEventHandler(MouseEvent.MOUSE_CLICKED, t -> {
                t.consume();
                if (row.isEmpty()) {
                    return;
                }

                var cm = new FileContextMenu(fileList.getFileSystemModel(), row.getItem(), editing);
                if (t.getButton() == MouseButton.SECONDARY) {
                    cm.show(row, t.getScreenX(), t.getScreenY());
                }
            });

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    fileList.onDoubleClick(row.getItem());
                }
            });

            fileList.getDraggedOverDirectory().addListener((observable, oldValue, newValue) -> {
                row.pseudoClassStateChanged(DRAG_OVER, newValue != null && newValue == row.getItem());
            });

            fileList.getDraggedOverEmpty().addListener((observable, oldValue, newValue) -> {
                table.pseudoClassStateChanged(DRAG_INTO_CURRENT, newValue);
            });

            row.setOnDragEntered(event -> {
                listEntry.get().onDragEntered(event);
            });
            row.setOnDragOver(event -> {
                listEntry.get().onDragOver(event);
            });
            row.setOnDragDetected(event -> {
                listEntry.get().startDrag(event);
            });
            row.setOnDragExited(event -> {
                listEntry.get().onDragExited(event);
            });
            row.setOnDragDropped(event -> {
                listEntry.get().onDragDrop(event);
            });

            return row;
        });

        SimpleChangeListener.apply(fileList.getShown(), (newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                table.getItems().setAll(newValue);
                if (newValue.size() > 0) {
                    table.scrollTo(0);
                }
            });
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

        public FilenameCell(Property<FileSystem.FileEntry> editing) {
            editing.addListener((observable, oldValue, newValue) -> {
                if (getTableRow().getItem() != null
                        && getTableRow().getItem().equals(newValue)) {
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
