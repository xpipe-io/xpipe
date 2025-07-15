package io.xpipe.app.browser.file;

import io.xpipe.app.comp.base.LazyTextFieldComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ContextMenuHelper;
import io.xpipe.app.util.InputHelper;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FileKind;
import io.xpipe.core.FilePath;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import atlantafx.base.controls.Spacer;

class BrowserFileListNameCell extends TableCell<BrowserEntry, String> {

    private final BrowserFileListModel fileList;
    private final ObservableStringValue typedSelection;
    private final StringProperty img = new SimpleStringProperty();
    private final StringProperty text = new SimpleStringProperty();

    private final BooleanProperty updating = new SimpleBooleanProperty();

    public BrowserFileListNameCell(
            BrowserFileListModel fileList,
            ObservableStringValue typedSelection,
            Property<BrowserEntry> editing,
            TableView<BrowserEntry> tableView) {
        this.fileList = fileList;
        this.typedSelection = typedSelection;

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
                .getTextField();
        var quickAccess = createQuickAccessButton();
        setupShortcuts(tableView, (ButtonBase) quickAccess);
        setupRename(fileList, textField, editing);

        Node imageView = PrettyImageHelper.ofFixedSize(img, 24, 24).createRegion();
        HBox graphic = new HBox(imageView, new Spacer(5), quickAccess, new Spacer(1), textField);
        quickAccess.prefHeightProperty().bind(graphic.heightProperty());
        graphic.setAlignment(Pos.CENTER_LEFT);
        graphic.setPrefHeight(34);
        HBox.setHgrow(textField, Priority.ALWAYS);
        graphic.setAlignment(Pos.CENTER_LEFT);
        setGraphic(graphic);
    }

    private Region createQuickAccessButton() {
        var quickAccess = new BrowserQuickAccessButtonComp(() -> getTableRow().getItem(), fileList.getFileSystemModel())
                .hide(Bindings.createBooleanBinding(
                        () -> {
                            if (getTableRow() == null) {
                                return true;
                            }

                            var item = getTableRow().getItem();
                            if (item == null) {
                                return false;
                            }

                            var notDir = item.getRawFileEntry().resolved().getKind() != FileKind.DIRECTORY;
                            var isParentLink = item.getRawFileEntry()
                                    .equals(fileList.getFileSystemModel().getCurrentParentDirectory());
                            return notDir || isParentLink;
                        },
                        itemProperty()))
                .focusTraversable(false)
                .createRegion();
        return quickAccess;
    }

    private void setupShortcuts(TableView<BrowserEntry> tableView, ButtonBase quickAccess) {
        InputHelper.onExactKeyCode(tableView, KeyCode.RIGHT, false, event -> {
            var selected = fileList.getSelection();
            if (selected.size() == 1 && selected.getFirst() == getTableRow().getItem()) {
                quickAccess.fire();
                event.consume();
            }
        });
        InputHelper.onExactKeyCode(tableView, KeyCode.SPACE, true, event -> {
            // Don't show when renaming files
            if (fileList.getEditing().getValue() != null) {
                return;
            }

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

    private void setupRename(BrowserFileListModel fileList, TextField textField, Property<BrowserEntry> editing) {
        ChangeListener<String> listener = (observable, oldValue, newValue) -> {
            if (updating.get()) {
                return;
            }

            getTableRow().requestFocus();
            var it = getTableRow().getItem();
            editing.setValue(null);
            ThreadHelper.runFailableAsync(() -> {
                if (it == null) {
                    return;
                }

                var r = fileList.rename(it, newValue);
                Platform.runLater(() -> {
                    updateItem(getItem(), isEmpty());
                    fileList.getSelection().setAll(r);
                    getTableView().scrollTo(r);
                });
            });
        };
        text.addListener(listener);

        editing.addListener((observable, oldValue, newValue) -> {
            var item = getTableRow().getItem();
            if (item != null && item.equals(newValue)) {
                PlatformThread.runLaterIfNeeded(() -> {
                    textField.setDisable(false);
                    textField.requestFocus();

                    var content = textField.getText();
                    if (content != null && !content.isEmpty()) {
                        var name = FilePath.of(content);
                        var baseNameEnd = item.getRawFileEntry().getKind() == FileKind.DIRECTORY
                                ? content.length()
                                : name.getBaseName().toString().length();
                        textField.selectRange(0, baseNameEnd);
                    }
                });
            }
        });

        textField.disabledProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue && newValue) {
                Platform.runLater(() -> {
                    editing.setValue(null);
                });
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
                pseudoClassStateChanged(PseudoClass.getPseudoClass("folder"), isDirectory);

                var normalName = getTableRow().getItem().getRawFileEntry().getKind() == FileKind.LINK
                        ? getTableRow().getItem().getFileName() + " -> "
                                + getTableRow()
                                        .getItem()
                                        .getRawFileEntry()
                                        .resolved()
                                        .getPath()
                        : getTableRow().getItem().getFileName();
                var fileName = normalName;
                var hidden = getTableRow().getItem().getRawFileEntry().getInfo().explicitlyHidden()
                        || fileName.startsWith(".");
                getTableRow().pseudoClassStateChanged(PseudoClass.getPseudoClass("hidden"), hidden);
                text.set(fileName);
                // Visibility seems to be bugged, so use opacity
                setOpacity(1.0);
            }
        }
    }
}
