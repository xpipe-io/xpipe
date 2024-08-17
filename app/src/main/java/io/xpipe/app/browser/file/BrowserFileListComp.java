package io.xpipe.app.browser.file;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.comp.base.LazyTextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.*;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FileInfo;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileNames;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static io.xpipe.app.util.HumanReadableFormat.byteCount;
import static javafx.scene.control.TableColumn.SortType.ASCENDING;

public final class BrowserFileListComp extends SimpleComp {

    private static final PseudoClass HIDDEN = PseudoClass.getPseudoClass("hidden");
    private static final PseudoClass EMPTY = PseudoClass.getPseudoClass("empty");
    private static final PseudoClass FILE = PseudoClass.getPseudoClass("file");
    private static final PseudoClass FOLDER = PseudoClass.getPseudoClass("folder");
    private static final PseudoClass DRAG = PseudoClass.getPseudoClass("drag");
    private static final PseudoClass DRAG_OVER = PseudoClass.getPseudoClass("drag-over");
    private static final PseudoClass DRAG_INTO_CURRENT = PseudoClass.getPseudoClass("drag-into-current");

    private final BrowserFileListModel fileList;
    private final StringProperty typedSelection = new SimpleStringProperty("");
    private final DoubleProperty ownerWidth = new SimpleDoubleProperty();

    public BrowserFileListComp(BrowserFileListModel fileList) {
        this.fileList = fileList;
    }

    @Override
    protected Region createSimple() {
        return createTable();
    }

    @SuppressWarnings("unchecked")
    private TableView<BrowserEntry> createTable() {
        var filenameCol = new TableColumn<BrowserEntry, String>();
        filenameCol.textProperty().bind(AppI18n.observable("name"));
        filenameCol.setCellValueFactory(param -> new SimpleStringProperty(
                param.getValue() != null
                        ? FileNames.getFileName(
                                param.getValue().getRawFileEntry().getPath())
                        : null));
        filenameCol.setComparator(Comparator.comparing(String::toLowerCase));
        filenameCol.setSortType(ASCENDING);
        filenameCol.setCellFactory(col -> new FilenameCell(fileList.getEditing(), col.getTableView()));
        filenameCol.setReorderable(false);

        var sizeCol = new TableColumn<BrowserEntry, Number>();
        sizeCol.textProperty().bind(AppI18n.observable("size"));
        sizeCol.setCellValueFactory(param -> new SimpleLongProperty(
                param.getValue().getRawFileEntry().resolved().getSize()));
        sizeCol.setCellFactory(col -> new FileSizeCell());
        sizeCol.setResizable(false);
        sizeCol.setPrefWidth(120);
        sizeCol.setReorderable(false);

        var mtimeCol = new TableColumn<BrowserEntry, Instant>();
        mtimeCol.textProperty().bind(AppI18n.observable("modified"));
        mtimeCol.setCellValueFactory(param -> new SimpleObjectProperty<>(
                param.getValue().getRawFileEntry().resolved().getDate()));
        mtimeCol.setCellFactory(col -> new FileTimeCell());
        mtimeCol.setResizable(false);
        mtimeCol.setPrefWidth(150);
        mtimeCol.setReorderable(false);

        var modeCol = new TableColumn<BrowserEntry, String>();
        modeCol.textProperty().bind(AppI18n.observable("attributes"));
        modeCol.setCellValueFactory(param -> new SimpleObjectProperty<>(
                param.getValue().getRawFileEntry().resolved().getInfo() instanceof FileInfo.Unix u ? u.getPermissions() : null));
        modeCol.setCellFactory(col -> new FileModeCell());
        modeCol.setResizable(false);
        modeCol.setPrefWidth(120);
        modeCol.setSortable(false);
        modeCol.setReorderable(false);

        var ownerCol = new TableColumn<BrowserEntry, String>();
        ownerCol.textProperty().bind(AppI18n.observable("owner"));
        ownerCol.setCellValueFactory(param -> {
            return new SimpleObjectProperty<>(formatOwner(param.getValue()));
        });
        ownerCol.setCellFactory(col -> new FileOwnerCell());
        ownerCol.setSortable(false);
        ownerCol.setReorderable(false);
        ownerCol.prefWidthProperty().bind(ownerWidth);
        ownerCol.setResizable(false);

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
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        table.setFixedCellSize(32.0);

        table.widthProperty().subscribe((newValue) -> {
            ownerCol.setVisible(newValue.doubleValue() > 1000);
        });

        prepareTableSelectionModel(table);
        prepareTableShortcuts(table);
        prepareTableEntries(table);
        prepareTableChanges(table, mtimeCol, modeCol, ownerCol);
        prepareTypedSelectionModel(table);

        return table;
    }

    private String formatOwner(BrowserEntry param) {
        FileInfo.Unix unix = param.getRawFileEntry().resolved().getInfo() instanceof FileInfo.Unix u ? u : null;
        if (unix == null) {
            return null;
        }

        var m = fileList.getFileSystemModel();
        var user = unix.getUser() != null ? unix.getUser() : m.getCache().getUsers().get(unix.getUid());
        var group = unix.getGroup() != null ? unix.getGroup() : m.getCache().getGroups().get(unix.getGid());
        var uid = String.valueOf(unix.getUid() != null ? unix.getUid() : m.getCache().getUidForUser(user)).replaceAll("000$", "k");
        var gid = String.valueOf(unix.getGid() != null ? unix.getGid() : m.getCache().getGidForGroup(group)).replaceAll("000$", "k");
        if (uid.equals(gid)) {
            return user + " [" + uid + "]";
        }
        return user + " [" + uid + "] / " + group + " [" + gid + "]";
    }

    private void prepareTypedSelectionModel(TableView<BrowserEntry> table) {
        AtomicReference<Instant> lastFail = new AtomicReference<>();
        table.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            updateTypedSelection(table, lastFail, event, false);
        });

        table.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            typedSelection.set("");
            lastFail.set(null);
        });

        fileList.getFileSystemModel().getCurrentPath().addListener((observable, oldValue, newValue) -> {
            typedSelection.set("");
            lastFail.set(null);
        });

        table.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                typedSelection.set("");
                lastFail.set(null);
            }
        });
    }

    private void updateTypedSelection(
            TableView<BrowserEntry> table, AtomicReference<Instant> lastType, KeyEvent event, boolean recursive) {
        var typed = event.getText();
        if (typed.isEmpty()) {
            return;
        }

        var updated = typedSelection.get() + typed;
        var found = fileList.getShown().getValue().stream()
                .filter(browserEntry -> browserEntry.getFileName().toLowerCase().startsWith(updated.toLowerCase()))
                .findFirst();
        if (found.isEmpty()) {
            if (typedSelection.get().isEmpty()) {
                return;
            }

            var inCooldown = lastType.get() != null
                    && Duration.between(lastType.get(), Instant.now()).toMillis() < 1000;
            if (inCooldown) {
                lastType.set(Instant.now());
                event.consume();
                return;
            } else {
                lastType.set(null);
                typedSelection.set("");
                table.getSelectionModel().clearSelection();
                if (!recursive) {
                    updateTypedSelection(table, lastType, event, true);
                }
                return;
            }
        }

        lastType.set(Instant.now());
        typedSelection.set(updated);
        table.scrollTo(found.get());
        table.getSelectionModel().clearAndSelect(fileList.getShown().getValue().indexOf(found.get()));
        event.consume();
    }

    private void prepareTableSelectionModel(TableView<BrowserEntry> table) {
        if (!fileList.getSelectionMode().isMultiple()) {
            table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        } else {
            table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }
        table.getSelectionModel().setCellSelectionEnabled(false);

        table.getSelectionModel().getSelectedItems().addListener((ListChangeListener<? super BrowserEntry>) c -> {
            fileList.getSelection().setAll(c.getList());
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

                var indices = c.getList().stream()
                        .mapToInt(entry -> table.getItems().indexOf(entry))
                        .toArray();
                table.getSelectionModel()
                        .selectIndices(table.getItems().indexOf(c.getList().getFirst()), indices);
            });
        });
    }

    private void prepareTableShortcuts(TableView<BrowserEntry> table) {
        table.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            var selected = fileList.getSelection();
            var action = BrowserAction.getFlattened(fileList.getFileSystemModel(), selected).stream()
                    .filter(browserAction -> browserAction.isApplicable(fileList.getFileSystemModel(), selected)
                            && browserAction.isActive(fileList.getFileSystemModel(), selected))
                    .filter(browserAction -> browserAction.getShortcut() != null)
                    .filter(browserAction -> browserAction.getShortcut().match(event))
                    .findAny();
            action.ifPresent(browserAction -> {
                ThreadHelper.runFailableAsync(() -> {
                    browserAction.execute(fileList.getFileSystemModel(), selected);
                });
                event.consume();
            });
            if (action.isPresent()) {
                return;
            }

            if (event.getCode() == KeyCode.ESCAPE) {
                table.getSelectionModel().clearSelection();
                event.consume();
            }
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
        table.setOnDragDone(event -> {
            emptyEntry.onDragDone(event);
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
            row.accessibleTextProperty()
                    .bind(Bindings.createStringBinding(
                            () -> {
                                return row.getItem() != null ? row.getItem().getFileName() : null;
                            },
                            row.itemProperty()));
            row.focusTraversableProperty()
                    .bind(Bindings.createBooleanBinding(
                            () -> {
                                return row.getItem() != null;
                            },
                            row.itemProperty()));
            var listEntry = Bindings.createObjectBinding(
                    () -> new BrowserFileListCompEntry(table, row, row.getItem(), fileList), row.itemProperty());

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
                row.pseudoClassStateChanged(
                        FILE, newValue != null && newValue.getRawFileEntry().getKind() != FileKind.DIRECTORY);
                row.pseudoClassStateChanged(
                        FOLDER, newValue != null && newValue.getRawFileEntry().getKind() == FileKind.DIRECTORY);
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
            row.setOnDragDone(event -> {
                listEntry.get().onDragDone(event);
            });

            return row;
        });
    }

    private void prepareTableChanges(
            TableView<BrowserEntry> table,
            TableColumn<BrowserEntry, Instant> mtimeCol,
            TableColumn<BrowserEntry, String> modeCol,
            TableColumn<BrowserEntry, String> ownerCol) {
        var lastDir = new SimpleObjectProperty<FileEntry>();
        Runnable updateHandler = () -> {
            Platform.runLater(() -> {
                var newItems = new ArrayList<>(fileList.getShown().getValue());

                var hasModifiedDate = newItems.size() == 0
                        || newItems.stream()
                                .anyMatch(entry -> entry.getRawFileEntry().getDate() != null);
                if (!hasModifiedDate) {
                    table.getColumns().remove(mtimeCol);
                } else {
                    if (!table.getColumns().contains(mtimeCol)) {
                        table.getColumns().add(mtimeCol);
                    }
                }

                ownerWidth.set(fileList.getAll().getValue().stream()
                        .map(browserEntry -> formatOwner(browserEntry))
                        .map(s -> s != null ? s.length() * 10 : 0)
                        .max(Comparator.naturalOrder()).orElse(150));
                if (fileList.getFileSystemModel().getFileSystem() != null) {
                    var shell = fileList.getFileSystemModel()
                            .getFileSystem()
                            .getShell()
                            .orElseThrow();
                    var notWindows = !OsType.WINDOWS.equals(shell.getOsType());
                    if (!notWindows) {
                        table.getColumns().remove(modeCol);
                        table.getColumns().remove(ownerCol);
                    } else {
                        if (!table.getColumns().contains(modeCol)) {
                            table.getColumns().add(modeCol);
                        }
                        if (!table.getColumns().contains(ownerCol)) {
                            table.getColumns().add(table.getColumns().size() - 1, ownerCol);
                        } else {
                            table.getColumns().remove(ownerCol);
                        }
                    }
                }

                if (!table.getItems().equals(newItems)) {
                    // Sort the list ourselves as sorting the table would incur a lot of cell updates
                    var obs = FXCollections.observableList(newItems);
                    table.getItems().setAll(obs);
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

        if (!vbar.isVisible()) {
            return;
        }

        double proximity = 100;
        Bounds tableBounds = tableView.localToScene(tableView.getBoundsInLocal());
        double dragY = event.getSceneY();
        // Include table header as well in calculations
        double topYProximity = tableBounds.getMinY() + proximity + 20;
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

    private static class FileOwnerCell extends TableCell<BrowserEntry, String> {

        public FileOwnerCell() {
            setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        }

        @Override
        protected void updateItem(String owner, boolean empty) {
            super.updateItem(owner, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setText(null);
            } else {
                setText(owner);
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
                setText(
                        fileTime != null
                                ? HumanReadableFormat.date(
                                        fileTime.atZone(ZoneId.systemDefault()).toLocalDateTime())
                                : "");
            }
        }
    }

    private class FilenameCell extends TableCell<BrowserEntry, String> {

        private final StringProperty img = new SimpleStringProperty();
        private final StringProperty text = new SimpleStringProperty();

        private final BooleanProperty updating = new SimpleBooleanProperty();

        public FilenameCell(Property<BrowserEntry> editing, TableView<BrowserEntry> tableView) {
            accessibleTextProperty()
                    .bind(Bindings.createStringBinding(
                            () -> {
                                return getItem() != null ? getItem() : null;
                            },
                            itemProperty()));
            setAccessibleRole(AccessibleRole.TEXT);

            var textField = new LazyTextFieldComp(text)
                    .minWidth(USE_PREF_SIZE)
                    .createStructure()
                    .get();
            var quickAccess = new BrowserQuickAccessButtonComp(
                            () -> getTableRow().getItem(), fileList.getFileSystemModel())
                    .hide(Bindings.createBooleanBinding(
                                    () -> {
                                        var item = getTableRow().getItem();
                                        var notDir = item.getRawFileEntry()
                                                        .resolved()
                                                        .getKind()
                                                != FileKind.DIRECTORY;
                                        var isParentLink = item.getRawFileEntry()
                                                .equals(fileList.getFileSystemModel()
                                                        .getCurrentParentDirectory());
                                        return notDir || isParentLink;
                                    },
                                    itemProperty())
                            .not()
                            .not())
                    .focusTraversable(false)
                    .createRegion();

            editing.addListener((observable, oldValue, newValue) -> {
                if (getTableRow().getItem() != null && getTableRow().getItem().equals(newValue)) {
                    PlatformThread.runLaterIfNeeded(() -> textField.requestFocus());
                }
            });

            ChangeListener<String> listener = (observable, oldValue, newValue) -> {
                if (updating.get()) {
                    return;
                }

                getTableRow().requestFocus();
                var it = getTableRow().getItem();
                editing.setValue(null);
                ThreadHelper.runAsync(() -> {
                    var r = fileList.rename(it, newValue);
                    Platform.runLater(() -> {
                        updateItem(getItem(), isEmpty());
                        fileList.getSelection().setAll(r);
                        getTableView().scrollTo(r);
                    });
                });
            };
            text.addListener(listener);

            Node imageView = PrettyImageHelper.ofFixedSize(img, 24, 24).createRegion();
            HBox graphic = new HBox(imageView, new Spacer(5), quickAccess, new Spacer(1), textField);
            quickAccess.prefHeightProperty().bind(graphic.heightProperty());
            graphic.setAlignment(Pos.CENTER_LEFT);
            graphic.setPrefHeight(34);
            HBox.setHgrow(textField, Priority.ALWAYS);
            graphic.setAlignment(Pos.CENTER_LEFT);
            setGraphic(graphic);

            InputHelper.onExactKeyCode(tableView, KeyCode.RIGHT, false, event -> {
                var selected = fileList.getSelection();
                if (selected.size() == 1 && selected.getFirst() == getTableRow().getItem()) {
                    ((ButtonBase) quickAccess).fire();
                    event.consume();
                }
            });
            InputHelper.onExactKeyCode(tableView, KeyCode.SPACE, true, event -> {
                var selection = typedSelection.get() + " ";
                var found = fileList.getShown().getValue().stream()
                        .filter(browserEntry ->
                                browserEntry.getFileName().toLowerCase().startsWith(selection))
                        .findFirst();
                // Ugly fix to prevent space from showing the menu when there is a file matching
                // Due to the table view input map, these events always get sent and consumed, not allowing us to
                // differentiate between these cases
                if (found.isPresent()) {
                    return;
                }

                var selected = fileList.getSelection();
                // Only show one menu across all selected entries
                if (selected.size() > 0 && selected.getLast() == getTableRow().getItem()) {
                    var cm = new BrowserContextMenu(
                            fileList.getFileSystemModel(), getTableRow().getItem(), false);
                    ContextMenuHelper.toggleShow(cm, this, Side.RIGHT);
                    event.consume();
                }
            });
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

                    // Visibility seems to be bugged, so use opacity
                    setOpacity(0.0);
                } else {
                    img.set(getTableRow().getItem().getIcon());

                    var isDirectory = getTableRow().getItem().getRawFileEntry().getKind() == FileKind.DIRECTORY;
                    pseudoClassStateChanged(FOLDER, isDirectory);

                    var normalName = getTableRow().getItem().getRawFileEntry().getKind() == FileKind.LINK
                            ? getTableRow().getItem().getFileName() + " -> "
                                    + getTableRow()
                                            .getItem()
                                            .getRawFileEntry()
                                            .resolved()
                                            .getPath()
                            : getTableRow().getItem().getFileName();
                    var fileName = normalName;
                    var hidden = getTableRow().getItem().getRawFileEntry().getInfo().explicitlyHidden() || fileName.startsWith(".");
                    getTableRow().pseudoClassStateChanged(HIDDEN, hidden);
                    text.set(fileName);
                    // Visibility seems to be bugged, so use opacity
                    setOpacity(1.0);
                }
            }
        }
    }
}
