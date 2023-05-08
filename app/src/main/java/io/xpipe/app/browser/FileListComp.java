/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import io.xpipe.app.browser.icon.FileIconManager;
import io.xpipe.app.comp.base.LazyTextFieldComp;
import io.xpipe.app.fxcomps.impl.SvgCacheComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.util.BusyProperty;
import io.xpipe.app.util.Containers;
import io.xpipe.app.util.HumanReadableFormat;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileSystem;
import javafx.application.Platform;
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

        var modeCol = new TableColumn<FileSystem.FileEntry, String>("Attributes");
        modeCol.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getMode()));
        modeCol.setCellFactory(col -> new FileModeCell());

        var table = new TableView<FileSystem.FileEntry>();
        table.setPlaceholder(new Region());
        table.getStyleClass().add(Styles.STRIPED);
        table.getColumns().setAll(filenameCol, sizeCol, modeCol, mtimeCol);
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

            Comparator<? super FileSystem.FileEntry> us =
                    parentFirst.thenComparing(dirsFirst).thenComparing(comp);
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
            // Explicitly unselect synthetic entries since we can't use a custom selection model as that is bugged in JavaFX
                    var toSelect = c.getList().stream()
                            .filter(entry -> fileList.getFileSystemModel().getCurrentParentDirectory() == null
                                    || !entry.getPath()
                                            .equals(fileList.getFileSystemModel()
                                                    .getCurrentParentDirectory()
                                                    .getPath()))
                            .toList();
                    fileList.getSelected().setAll(toSelect);
                    fileList.getFileSystemModel()
                            .getBrowserModel()
                            .getSelectedFiles()
                            .setAll(toSelect);

                    Platform.runLater(() -> {
                        var toUnselect = table.getSelectionModel().getSelectedItems().stream()
                                .filter(entry -> !toSelect.contains(entry))
                                .toList();
                        toUnselect.forEach(entry -> table.getSelectionModel()
                                .clearSelection(table.getItems().indexOf(entry)));
                    });
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

        prepareTableEntries(table);
        prepareTableChanges(table, mtimeCol, modeCol);

        return table;
    }

    private void prepareTableEntries(TableView<FileSystem.FileEntry> table) {
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
    }
    private void prepareTableChanges(TableView<FileSystem.FileEntry> table, TableColumn<FileSystem.FileEntry, Instant> mtimeCol, TableColumn<FileSystem.FileEntry, String> modeCol) {
        var lastDir = new SimpleObjectProperty<FileSystem.FileEntry>();
        Runnable updateHandler = () -> {
            PlatformThread.runLaterIfNeeded(() -> {
                var newItems = new ArrayList<FileSystem.FileEntry>();
                var parentEntry = fileList.getFileSystemModel().getCurrentParentDirectory();
                if (parentEntry != null) {
                    newItems.add(parentEntry);
                }
                newItems.addAll(fileList.getShown().getValue());

                var hasModifiedDate =
                        newItems.size() == 0 || newItems.stream().anyMatch(entry -> entry.getDate() != null);
                if (!hasModifiedDate) {
                    table.getColumns().remove(mtimeCol);
                } else {
                    if (!table.getColumns().contains(mtimeCol)) {
                        table.getColumns().add(mtimeCol);
                    }
                }

                var hasAttributes = fileList.getFileSystemModel().getFileSystem() != null
                        && !fileList.getFileSystemModel()
                                .getFileSystem()
                                .getShell()
                                .orElseThrow()
                                .getOsType()
                                .equals(OsType.WINDOWS);
                if (!hasAttributes) {
                    table.getColumns().remove(modeCol);
                } else {
                    if (!table.getColumns().contains(modeCol)) {
                        table.getColumns().add(modeCol);
                    }
                }

                if (!table.getItems().equals(newItems)) {
                    table.getItems().setAll(newItems);
                }

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
        };
        updateHandler.run();
        fileList.getShown().addListener((observable, oldValue, newValue) -> {
            updateHandler.run();
        });
        fileList.getFileSystemModel().getCurrentPath().addListener((observable, oldValue, newValue) -> {
            if (oldValue == null) {
                updateHandler.run();
            }
        });
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
        private final Node imageView = new SvgCacheComp(
                        new SimpleDoubleProperty(24), new SimpleDoubleProperty(24), img, FileIconManager.getSvgCache())
                .createRegion();
        private final StackPane textField =
                new LazyTextFieldComp(text).createStructure().get();
        private final ChangeListener<String> listener;

        private final BooleanProperty updating = new SimpleBooleanProperty();

        public FilenameCell(Property<FileSystem.FileEntry> editing) {
            editing.addListener((observable, oldValue, newValue) -> {
                if (getTableRow().getItem() != null && getTableRow().getItem().equals(newValue)) {
                    textField.requestFocus();
                }
            });

            listener = (observable, oldValue, newValue) -> {
                if (updating.get()) {
                    return;
                }

                fileList.rename(oldValue, newValue);
                textField.getScene().getRoot().requestFocus();
                editing.setValue(null);
                updateItem(getItem(), isEmpty());
            };
            text.addListener(listener);
        }

        @Override
        protected void updateItem(String fullPath, boolean empty) {
            if (updating.get()) {
                super.updateItem(fullPath, empty);
                return;
            }

            try (var b = new BusyProperty(updating)) {
                super.updateItem(fullPath, empty);
                setText(null);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    // Don't set image as that would trigger image comp update
                    // and cells are emptied on each change, leading to unnecessary changes
                    // img.set(null);
                    setGraphic(null);
                } else {
                    var box = new HBox(imageView, textField);
                    box.setSpacing(10);
                    box.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(textField, Priority.ALWAYS);
                    setGraphic(box);

                    var isParentLink = getTableRow()
                            .getItem()
                            .equals(fileList.getFileSystemModel().getCurrentParentDirectory());
                    img.set(FileIconManager.getFileIcon(
                            isParentLink
                                    ? fileList.getFileSystemModel().getCurrentDirectory()
                                    : getTableRow().getItem(),
                            isParentLink));

                    var isDirectory = getTableRow().getItem().isDirectory();
                    pseudoClassStateChanged(FOLDER, isDirectory);

                    var fileName = isParentLink ? ".." : FileNames.getFileName(fullPath);
                    var hidden = !isParentLink && (getTableRow().getItem().isHidden() || fileName.startsWith("."));
                    getTableRow().pseudoClassStateChanged(HIDDEN, hidden);
                    text.set(fileName);
                }
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

    private class FileModeCell extends TableCell<FileSystem.FileEntry, String> {

        @Override
        protected void updateItem(String mode, boolean empty) {
            super.updateItem(mode, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setText(null);
            } else {
                setText(mode);
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
