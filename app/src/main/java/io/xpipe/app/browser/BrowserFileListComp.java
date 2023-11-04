package io.xpipe.app.browser;

import atlantafx.base.theme.Styles;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.icon.FileIconManager;
import io.xpipe.app.comp.base.LazyTextFieldComp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.impl.PrettySvgComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.HumanReadableFormat;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FileSystem;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

import static io.xpipe.app.util.HumanReadableFormat.byteCount;
import static javafx.scene.control.TableColumn.SortType.ASCENDING;

final class BrowserFileListComp extends SimpleComp {

    private static final PseudoClass HIDDEN = PseudoClass.getPseudoClass("hidden");
    private static final PseudoClass EMPTY = PseudoClass.getPseudoClass("empty");
    private static final PseudoClass FILE = PseudoClass.getPseudoClass("file");
    private static final PseudoClass FOLDER = PseudoClass.getPseudoClass("folder");
    private static final PseudoClass DRAG = PseudoClass.getPseudoClass("drag");
    private static final PseudoClass DRAG_OVER = PseudoClass.getPseudoClass("drag-over");
    private static final PseudoClass DRAG_INTO_CURRENT = PseudoClass.getPseudoClass("drag-into-current");

    private final BrowserFileListModel fileList;

    public BrowserFileListComp(BrowserFileListModel fileList) {
        this.fileList = fileList;
    }

    @Override
    protected Region createSimple() {
        return createTable();
    }

    @SuppressWarnings("unchecked")
    private TableView<BrowserEntry> createTable() {
        var filenameCol = new TableColumn<BrowserEntry, String>("Name");
        filenameCol.setCellValueFactory(param -> new SimpleStringProperty(
                param.getValue() != null ? FileNames.getFileName(param.getValue().getRawFileEntry().getPath()) : null));
        filenameCol.setComparator(Comparator.comparing(String::toLowerCase));
        filenameCol.setSortType(ASCENDING);
        filenameCol.setCellFactory(col -> new FilenameCell(fileList.getEditing()));

        var sizeCol = new TableColumn<BrowserEntry, Number>("Size");
        sizeCol.setCellValueFactory(param -> new SimpleLongProperty(param.getValue().getRawFileEntry().resolved().getSize()));
        sizeCol.setCellFactory(col -> new FileSizeCell());

        var mtimeCol = new TableColumn<BrowserEntry, Instant>("Modified");
        mtimeCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getRawFileEntry().resolved().getDate()));
        mtimeCol.setCellFactory(col -> new FileTimeCell());

        var modeCol = new TableColumn<BrowserEntry, String>("Attributes");
        modeCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getRawFileEntry().resolved().getMode()));
        modeCol.setCellFactory(col -> new FileModeCell());
        modeCol.setSortable(false);

        var table = new TableView<BrowserEntry>();
        table.setAccessibleText("Directory contents");
        table.setPlaceholder(new Region());
        table.getStyleClass().add(Styles.STRIPED);
        table.getColumns().setAll(filenameCol, sizeCol, modeCol, mtimeCol);
        table.getSortOrder().add(filenameCol);
        table.setFocusTraversable(true);
        table.setSortPolicy(param -> {
            fileList.setComparator(table.getComparator());
            return true;
        });
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        filenameCol.minWidthProperty().bind(table.widthProperty().multiply(0.5));

        table.setFixedCellSize(34.0);

        prepareTableSelectionModel(table);
        prepareTableShortcuts(table);
        prepareTableEntries(table);
        prepareTableChanges(table, mtimeCol, modeCol);

        return table;
    }

    private void prepareTableSelectionModel(TableView<BrowserEntry> table) {
        if (!fileList.getMode().isMultiple()) {
            table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        } else {
            table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }

        table.getSelectionModel().setCellSelectionEnabled(false);

        table.getSelectionModel().getSelectedItems().addListener((ListChangeListener<? super BrowserEntry>) c -> {
            var toSelect = new ArrayList<>(c.getList());
            // Explicitly unselect synthetic entries since we can't use a custom selection model as that is bugged in
            // JavaFX
            toSelect.removeIf(entry -> fileList.getFileSystemModel().getCurrentParentDirectory() != null &&
                    entry.getRawFileEntry().getPath().equals(fileList.getFileSystemModel().getCurrentParentDirectory().getPath()));
            // Remove unsuitable selection
            toSelect.removeIf(
                    browserEntry -> (browserEntry.getRawFileEntry().getKind() == FileKind.DIRECTORY && !fileList.getMode().isAcceptsDirectories()) ||
                            (browserEntry.getRawFileEntry().getKind() != FileKind.DIRECTORY && !fileList.getMode().isAcceptsFiles()));
            fileList.getSelection().setAll(toSelect);

            Platform.runLater(() -> {
                var toUnselect = table.getSelectionModel().getSelectedItems().stream().filter(entry -> !toSelect.contains(entry)).toList();
                toUnselect.forEach(entry -> table.getSelectionModel().clearSelection(table.getItems().indexOf(entry)));
            });
        });

        fileList.getSelection().addListener((ListChangeListener<? super BrowserEntry>) c -> {
            if (c.getList().equals(table.getSelectionModel().getSelectedItems())) {
                return;
            }

            Platform.runLater(() -> {
                if (c.getList().isEmpty()) {
                    table.getSelectionModel().clearSelection();
                    return;
                }

                var indices = c.getList().stream().skip(1).mapToInt(entry -> table.getItems().indexOf(entry)).toArray();
                table.getSelectionModel().selectIndices(table.getItems().indexOf(c.getList().get(0)), indices);
            });
        });
    }

    private void prepareTableShortcuts(TableView<BrowserEntry> table) {
        table.setOnKeyPressed(event -> {
            var selected = fileList.getSelection();
            BrowserAction.getFlattened(fileList.getFileSystemModel(), selected).stream().filter(
                            browserAction -> browserAction.isApplicable(fileList.getFileSystemModel(), selected) &&
                                    browserAction.isActive(fileList.getFileSystemModel(), selected)).filter(
                            browserAction -> browserAction.getShortcut() != null).filter(browserAction -> browserAction.getShortcut().match(event)).findAny()
                    .ifPresent(browserAction -> {
                        ThreadHelper.runFailableAsync(() -> {
                            browserAction.execute(fileList.getFileSystemModel(), selected);
                        });
                    });
        });
    }

    private void prepareTableEntries(TableView<BrowserEntry> table) {
        var emptyEntry = new BrowserFileListCompEntry(table, table, null, fileList);
        table.setOnMouseClicked(e -> {
            emptyEntry.onMouseClick(e);
        });
        table.setOnMouseDragEntered(event -> {
            emptyEntry.onMouseDragEntered(event);
        });
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

        // Don't let the list view see this event
        // otherwise it unselects everything as it doesn't understand shift clicks
        table.addEventFilter(MouseEvent.MOUSE_CLICKED, t -> {
            if (t.getButton() == MouseButton.PRIMARY && t.isShiftDown() && t.getClickCount() == 1) {
                t.consume();
            }
        });

        table.setRowFactory(param -> {
            TableRow<BrowserEntry> row = new TableRow<>();
            row.accessibleTextProperty().bind(Bindings.createStringBinding(() -> {
                return row.getItem() != null ? row.getItem().getFileName() : null;
            }, row.itemProperty()));
            row.focusTraversableProperty().bind(Bindings.createBooleanBinding(() -> {
                return row.getItem() != null;
            }, row.itemProperty()));
            new ContextMenuAugment<>(event -> {
                if (row.getItem() == null) {
                    return event.getButton() == MouseButton.SECONDARY;
                }

                if (row.getItem() != null && row.getItem().getRawFileEntry().resolved().getKind() == FileKind.DIRECTORY) {
                    return event.getButton() == MouseButton.SECONDARY;
                }

                if (row.getItem() != null && row.getItem().getRawFileEntry().resolved().getKind() != FileKind.DIRECTORY) {
                    return event.getButton() == MouseButton.SECONDARY || event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2;
                }

                return false;
            }, () -> {
                if (row.getItem() != null && row.getItem().isSynthetic()) {
                    return null;
                }

                return new BrowserContextMenu(fileList.getFileSystemModel(), row.getItem());
            }).augment(new SimpleCompStructure<>(row));
            var listEntry = Bindings.createObjectBinding(() -> new BrowserFileListCompEntry(table, row, row.getItem(), fileList), row.itemProperty());

            // Don't let the list view see this event
            // otherwise it unselects everything as it doesn't understand shift clicks
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, t -> {
                if (t.getButton() == MouseButton.PRIMARY && t.isShiftDown()) {
                    listEntry.get().onMouseShiftClick(t);
                }
            });

            row.itemProperty().addListener((observable, oldValue, newValue) -> {
                row.pseudoClassStateChanged(DRAG, false);
                row.pseudoClassStateChanged(DRAG_OVER, false);
            });

            row.itemProperty().addListener((observable, oldValue, newValue) -> {
                row.pseudoClassStateChanged(EMPTY, newValue == null);
                row.pseudoClassStateChanged(FILE, newValue != null && newValue.getRawFileEntry().getKind() != FileKind.DIRECTORY);
                row.pseudoClassStateChanged(FOLDER, newValue != null && newValue.getRawFileEntry().getKind() == FileKind.DIRECTORY);
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
            row.setOnMouseDragEntered(event -> {
                listEntry.get().onMouseDragEntered(event);
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

    private void prepareTableChanges(
            TableView<BrowserEntry> table, TableColumn<BrowserEntry, Instant> mtimeCol, TableColumn<BrowserEntry, String> modeCol
    ) {
        var lastDir = new SimpleObjectProperty<FileSystem.FileEntry>();
        Runnable updateHandler = () -> {
            PlatformThread.runLaterIfNeeded(() -> {
                var newItems = new ArrayList<>(fileList.getShown().getValue());

                var hasModifiedDate = newItems.size() == 0 || newItems.stream().anyMatch(entry -> entry.getRawFileEntry().getDate() != null);
                if (!hasModifiedDate) {
                    table.getColumns().remove(mtimeCol);
                } else {
                    if (!table.getColumns().contains(mtimeCol)) {
                        table.getColumns().add(mtimeCol);
                    }
                }

                var hasAttributes = fileList.getFileSystemModel().getFileSystem() != null &&
                        !fileList.getFileSystemModel().getFileSystem().getShell().orElseThrow().getOsType().equals(OsType.WINDOWS);
                if (!hasAttributes) {
                    table.getColumns().remove(modeCol);
                } else {
                    if (!table.getColumns().contains(modeCol)) {
                        table.getColumns().add(modeCol);
                    }
                }

                if (!table.getItems().equals(newItems)) {
                    // Sort the list ourselves as sorting the table would incur a lot of cell updates
                    var obs = FXCollections.observableList(newItems);
                    table.getItems().setAll(obs);
                    // table.sort();
                }

                var currentDirectory = fileList.getFileSystemModel().getCurrentDirectory();
                if (!Objects.equals(lastDir.get(), currentDirectory)) {
                    TableViewSkin<?> skin = (TableViewSkin<?>) table.getSkin();
                    if (skin != null) {
                        VirtualFlow<?> flow = (VirtualFlow<?>) skin.getChildren().get(1);
                        ScrollBar vbar = (ScrollBar) flow.getChildrenUnmodifiable().get(2);
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

        // clamp new values between 0 and 1 to prevent scrollbar flicking around at the edges
        if (dragY < topYProximity) {
            var scrollValue = Math.min(topYProximity - dragY, 100) / 10000.0;
            vbar.setValue(Math.max(vbar.getValue() - scrollValue, 0));
        } else if (dragY > bottomYProximity) {
            var scrollValue = Math.min(dragY - bottomYProximity, 100) / 10000.0;
            vbar.setValue(Math.min(vbar.getValue() + scrollValue, 1.0));
        }
    }

    private static class FileSizeCell extends TableCell<BrowserEntry, Number> {

        @Override
        protected void updateItem(Number fileSize, boolean empty) {
            super.updateItem(fileSize, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setText(null);
            } else {
                var path = getTableRow().getItem();
                if (path.getRawFileEntry().resolved().getKind() == FileKind.DIRECTORY) {
                    setText("");
                } else {
                    setText(byteCount(fileSize.longValue()));
                }
            }
        }
    }

    private static class FileModeCell extends TableCell<BrowserEntry, String> {

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

    private static class FileTimeCell extends TableCell<BrowserEntry, Instant> {

        @Override
        protected void updateItem(Instant fileTime, boolean empty) {
            super.updateItem(fileTime, empty);
            if (empty) {
                setText(null);
            } else {
                setText(fileTime != null ? HumanReadableFormat.date(fileTime.atZone(ZoneId.systemDefault()).toLocalDateTime()) : "");
            }
        }
    }

    private class FilenameCell extends TableCell<BrowserEntry, String> {

        private final StringProperty img = new SimpleStringProperty();
        private final StringProperty text = new SimpleStringProperty();
        private final Node imageView = new PrettySvgComp(img, 24, 24).createRegion();
        private final StackPane textField = new LazyTextFieldComp(text).createStructure().get();
        private final HBox graphic;

        private final BooleanProperty updating = new SimpleBooleanProperty();

        public FilenameCell(Property<BrowserEntry> editing) {
            accessibleTextProperty().bind(Bindings.createStringBinding(() -> {
                return getItem() != null ? getItem() : null;
            }, itemProperty()));
            setAccessibleRole(AccessibleRole.TEXT);

            editing.addListener((observable, oldValue, newValue) -> {
                if (getTableRow().getItem() != null && getTableRow().getItem().equals(newValue)) {
                    PlatformThread.runLaterIfNeeded(() -> textField.requestFocus());
                }
            });

            ChangeListener<String> listener = (observable, oldValue, newValue) -> {
                if (updating.get()) {
                    return;
                }

                fileList.rename(oldValue, newValue);
                textField.getScene().getRoot().requestFocus();
                editing.setValue(null);
                updateItem(getItem(), isEmpty());
            };
            text.addListener(listener);

            graphic = new HBox(imageView, textField);
            graphic.setSpacing(10);
            graphic.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textField, Priority.ALWAYS);
            setGraphic(graphic);
        }

        @Override
        protected void updateItem(String newName, boolean empty) {
            if (updating.get()) {
                super.updateItem(newName, empty);
                return;
            }

            try (var ignored = new BooleanScope(updating).start()) {
                super.updateItem(newName, empty);
                if (empty || newName == null || getTableRow().getItem() == null) {
                    // Don't set image as that would trigger image comp update
                    // and cells are emptied on each change, leading to unnecessary changes
                    // img.set(null);

                    // Use opacity instead of visibility as visibility is kinda bugged with web views
                    setOpacity(0.0);
                } else {
                    var isParentLink = getTableRow().getItem().getRawFileEntry().equals(fileList.getFileSystemModel().getCurrentParentDirectory());
                    img.set(FileIconManager.getFileIcon(
                            isParentLink ? fileList.getFileSystemModel().getCurrentDirectory() : getTableRow().getItem().getRawFileEntry(),
                            isParentLink));

                    var isDirectory = getTableRow().getItem().getRawFileEntry().getKind() == FileKind.DIRECTORY;
                    pseudoClassStateChanged(FOLDER, isDirectory);

                    var normalName = getTableRow().getItem().getRawFileEntry().getKind() == FileKind.LINK ?
                            getTableRow().getItem().getFileName() + " -> " + getTableRow().getItem().getRawFileEntry().resolved().getPath() :
                            getTableRow().getItem().getFileName();
                    var fileName = isParentLink ? ".." : normalName;
                    var hidden = !isParentLink && (getTableRow().getItem().getRawFileEntry().isHidden() || fileName.startsWith("."));
                    getTableRow().pseudoClassStateChanged(HIDDEN, hidden);
                    text.set(fileName);

                    // Use opacity instead of visibility as visibility is kinda bugged with web views
                    setOpacity(1.0);
                }
            }
        }
    }
}
