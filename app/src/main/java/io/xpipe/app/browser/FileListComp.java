/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import io.xpipe.app.browser.icon.FileIconManager;
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
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

import static io.xpipe.app.util.HumanReadableFormat.byteCount;
import static javafx.scene.control.TableColumn.SortType.ASCENDING;

final class FileListComp extends AnchorPane {

    private static final PseudoClass HIDDEN = PseudoClass.getPseudoClass("hidden");
    private static final PseudoClass EMPTY = PseudoClass.getPseudoClass("empty");
    private static final PseudoClass FILE = PseudoClass.getPseudoClass("file");
    private static final PseudoClass FOLDER = PseudoClass.getPseudoClass("folder");
    private static final PseudoClass DRAG = PseudoClass.getPseudoClass("drag");
    private static final PseudoClass DRAG_OVER = PseudoClass.getPseudoClass("drag-over");
    private static final PseudoClass DRAG_INTO_CURRENT = PseudoClass.getPseudoClass("drag-into-current");

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
        var filenameCol = new TableColumn<FileSystem.FileEntry, String>("Name");
        filenameCol.setCellValueFactory(param -> new SimpleStringProperty(
                param.getValue() != null
                        ? FileNames.getFileName(param.getValue().getPath())
                        : null));
        filenameCol.setComparator(Comparator.comparing(String::toLowerCase));
        filenameCol.setSortType(ASCENDING);
        filenameCol.setCellFactory(col -> new FilenameCell(fileList.getEditing()));

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
        table.setSortPolicy(param -> {
            var comp = table.getComparator();
            if (comp == null) {
                return true;
            }

            var parentFirst = new Comparator<FileSystem.FileEntry>() {
                @Override
                public int compare(FileSystem.FileEntry o1, FileSystem.FileEntry o2) {
                    var c = fileList.getFileSystemModel().getCurrentParentDirectory();
                    if (c == null) {
                        return 0;
                    }

                    return o1.getPath().equals(c.getPath()) ? -1 : (o2.getPath().equals(c.getPath()) ? 1 : 0);
                }
            };
            var dirsFirst = Comparator.<FileSystem.FileEntry, Boolean>comparing(path -> !path.isDirectory());

            Comparator<? super FileSystem.FileEntry> us = parentFirst.thenComparing(dirsFirst).thenComparing(comp);
            FXCollections.sort(table.getItems(), us);
            return true;
        });
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        filenameCol.minWidthProperty().bind(table.widthProperty().multiply(0.5));

        if (fileList.getMode().equals(FileBrowserModel.Mode.SINGLE_FILE_CHOOSER)
                || fileList.getMode().equals(FileBrowserModel.Mode.DIRECTORY_CHOOSER)) {
            table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        } else {
            table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }

        table.getSelectionModel().getSelectedItems().addListener((ListChangeListener<? super FileSystem.FileEntry>)
                c -> {
                    fileList.getSelected().setAll(c.getList());
                    fileList.getFileSystemModel()
                            .getBrowserModel()
                            .getSelectedFiles()
                            .setAll(c.getList());
                });

        table.setOnKeyPressed(event -> {
            if (event.isControlDown()
                    && event.getCode().equals(KeyCode.C)
                    && table.getSelectionModel().getSelectedItems().size() > 0) {
                FileBrowserClipboard.startCopy(
                        fileList.getFileSystemModel().getCurrentDirectory(),
                        table.getSelectionModel().getSelectedItems());
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

        var emptyEntry = new FileListCompEntry(table, null, fileList);
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
            var listEntry = Bindings.createObjectBinding(
                    () -> new FileListCompEntry(row, row.getItem(), fileList), row.itemProperty());

            row.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue && listEntry.get().isSynthetic()) {
                    row.updateSelected(false);
                }
            });

            row.itemProperty().addListener((observable, oldValue, newValue) -> {
                row.pseudoClassStateChanged(DRAG, false);
                row.pseudoClassStateChanged(DRAG_OVER, false);
            });

            row.itemProperty().addListener((observable, oldValue, newValue) -> {
                row.pseudoClassStateChanged(EMPTY, newValue == null);
                row.pseudoClassStateChanged(FILE, newValue != null && !newValue.isDirectory());
                row.pseudoClassStateChanged(FOLDER, newValue != null && newValue.isDirectory());
            });

            fileList.getDraggedOverDirectory().addListener((observable, oldValue, newValue) -> {
                row.pseudoClassStateChanged(DRAG_OVER, newValue != null && newValue == row.getItem());
            });

            fileList.getDraggedOverEmpty().addListener((observable, oldValue, newValue) -> {
                table.pseudoClassStateChanged(DRAG_INTO_CURRENT, newValue);
            });

            row.setOnMouseClicked(e -> {
                listEntry.get().onMouseClick(e);
            });

            row.setOnDragEntered(event -> {
                listEntry.get().onDragEntered(event);
            });
            row.setOnDragOver(event -> {
                borderScroll(table, event);
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

        var lastDir = new SimpleObjectProperty<FileSystem.FileEntry>();
        SimpleChangeListener.apply(fileList.getShown(), (newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                var newItems = new ArrayList<FileSystem.FileEntry>();
                var parentEntry = fileList.getFileSystemModel().getCurrentParentDirectory();
                if (parentEntry != null) {
                    newItems.add(parentEntry);
                }
                newItems.addAll(newValue);

                var hasModifiedDate =
                        newItems.size() == 0 || newItems.stream().anyMatch(entry -> entry.getDate() != null);
                if (!hasModifiedDate) {
                    table.getColumns().remove(mtimeCol);
                } else {
                    if (!table.getColumns().contains(mtimeCol)) {
                        table.getColumns().add(mtimeCol);
                    }
                }

                table.getItems().setAll(newItems);

                var currentDirectory = fileList.getFileSystemModel().getCurrentDirectory();
                if (!Objects.equals(lastDir.get(), currentDirectory)) {
                    TableViewSkin<?> skin = (TableViewSkin<?>) table.getSkin();
                    if (skin != null) {
                        VirtualFlow<?> flow =
                                (VirtualFlow<?>) skin.getChildren().get(1);
                        ScrollBar vbar =
                                (ScrollBar) flow.getChildrenUnmodifiable().get(2);
                        if (vbar.getValue() != 0.0) {
                            table.scrollTo(0);
                        }
                    }
                }
                lastDir.setValue(currentDirectory);
            });
        });

        return table;
    }

    private void borderScroll(TableView<?> tableView, DragEvent event) {
        TableViewSkin<?> skin = (TableViewSkin<?>) tableView.getSkin();
        if (skin == null) {
            return;
        }

        VirtualFlow<?> flow = (VirtualFlow<?>) skin.getChildren().get(1);
        ScrollBar vbar = (ScrollBar) flow.getChildrenUnmodifiable().get(2);

        double proximity = 100;
        Bounds tableBounds = tableView.localToScene(tableView.getBoundsInParent());
        double dragY = event.getSceneY();
        double topYProximity = tableBounds.getMinY() + proximity;
        double bottomYProximity = tableBounds.getMaxY() - proximity;
        if (dragY < topYProximity) {
            var scrollValue = Math.min(topYProximity - dragY, 100) / 10000.0;
            vbar.setValue(vbar.getValue() - scrollValue);
        } else if (dragY > bottomYProximity) {
            var scrollValue = Math.min(dragY - bottomYProximity, 100) / 10000.0;
            vbar.setValue(vbar.getValue() + scrollValue);
        }
    }

    private class FilenameCell extends TableCell<FileSystem.FileEntry, String> {

        private final StringProperty img = new SimpleStringProperty();
        private final StringProperty text = new SimpleStringProperty();
        private final Node imageView = new PrettyImageComp(img, 24, 24).createRegion();
        private final StackPane textField =
                new LazyTextFieldComp(text).createStructure().get();
        private final ChangeListener<String> listener;

        public FilenameCell(Property<FileSystem.FileEntry> editing) {
            editing.addListener((observable, oldValue, newValue) -> {
                if (getTableRow().getItem() != null && getTableRow().getItem().equals(newValue)) {
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
                // Don't set image as that would trigger image comp update
                // and cells are emptied on each change, leading to unnecessary changes
                // img.set(null);
                setGraphic(null);
            } else {
                var isDirectory = getTableRow().getItem().isDirectory();
                var box = new HBox(imageView, textField);
                box.setSpacing(10);
                box.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(textField, Priority.ALWAYS);
                setGraphic(box);

                var isParentLink = getTableRow()
                        .getItem()
                        .equals(fileList.getFileSystemModel().getCurrentParentDirectory());
                img.set(FileIconManager.getFileIcon(isParentLink ? fileList.getFileSystemModel().getCurrentDirectory() : getTableRow().getItem(), isParentLink));

                pseudoClassStateChanged(FOLDER, isDirectory);

                var fileName = isParentLink
                        ? ".."
                        : FileNames.getFileName(fullPath);
                var hidden = !isParentLink && (getTableRow().getItem().isHidden() || fileName.startsWith("."));
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
                    setText("");
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
                                : "");
            }
        }
    }
}
