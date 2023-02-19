/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import io.xpipe.app.util.ExternalEditor;
import javafx.beans.property.Property;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;

final class FileContextMenu extends ContextMenu {

    private final OpenFileSystemModel model;
    private final String path;
    private final boolean directory;
    private final Property<String> editing;

    public FileContextMenu(OpenFileSystemModel model, String path, boolean directory, Property<String> editing) {
        super();
        this.model = model;
        this.path = path;
        this.directory = directory;
        this.editing = editing;
        createMenu();
    }

    private void createMenu() {
        var cut = new MenuItem("Delete");
        cut.setOnAction(event -> {
            event.consume();
            model.deleteAsync(path);
        });
        cut.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));

        var rename = new MenuItem("Rename");
        rename.setOnAction(event -> {
            event.consume();
            editing.setValue(path);
        });
        rename.setAccelerator(new KeyCodeCombination(KeyCode.F2));

        getItems().setAll(
                new SeparatorMenuItem(),
                cut,
                rename
        );

        if (directory) {
            var terminal = new MenuItem("Terminal");
            terminal.setOnAction(event -> {
                event.consume();
                model.openTerminalAsync(path);
            });
            getItems().add(0, terminal);
        } else {
            var open = new MenuItem("Edit");
            open.setOnAction(event -> {
                event.consume();
                ExternalEditor.get().openInEditor(model.getFileSystem(), path);
            });
            getItems().add(0, open);
        }
    }
}
