/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.ExternalEditor;
import io.xpipe.app.util.TerminalHelper;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.FileSystem;
import javafx.beans.property.Property;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import org.apache.commons.io.FilenameUtils;

import java.util.List;

final class FileContextMenu extends ContextMenu {

    public boolean isScript(FileSystem.FileEntry e) {
        if (e.isDirectory()) {
            return false;
        }

        var shell = e.getFileSystem().getShell();
        if (shell.isEmpty()) {
            return false;
        }

        var os = shell.get().getOsType();
        var ending = FilenameUtils.getExtension(e.getPath()).toLowerCase();
        if (os.equals(OsType.WINDOWS) && List.of("bat", "ps1", "cmd").contains(ending)) {
            return true;
        }

        return false;
    }

    private final OpenFileSystemModel model;
    private final FileSystem.FileEntry entry;
    private final Property<String> editing;

    public FileContextMenu(OpenFileSystemModel model, FileSystem.FileEntry entry, Property<String> editing) {
        super();
        this.model = model;
        this.entry = entry;
        this.editing = editing;
        createMenu();
    }

    private void createMenu() {
        var cut = new MenuItem("Delete");
        cut.setOnAction(event -> {
            event.consume();
            model.deleteAsync(entry.getPath());
        });
        cut.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));

        var rename = new MenuItem("Rename");
        rename.setOnAction(event -> {
            event.consume();
            editing.setValue(entry.getPath());
        });
        rename.setAccelerator(new KeyCodeCombination(KeyCode.F2));

        getItems().setAll(
                new SeparatorMenuItem(),
                cut,
                rename
        );

        if (entry.isDirectory()) {
            var terminal = new MenuItem("Terminal");
            terminal.setOnAction(event -> {
                event.consume();
                model.openTerminalAsync(entry.getPath());
            });
            getItems().add(0, terminal);
        } else {
            var open = new MenuItem("Open");
            open.setOnAction(event -> {
                event.consume();
                ExternalEditor.get().openInEditor(model.getFileSystem(), entry.getPath());
            });
            getItems().add(0, open);

            if (isScript(entry)) {
                var executeInBackground = new MenuItem("Run in background");
                executeInBackground.setOnAction(event -> {
                    event.consume();
                    ExternalEditor.get().openInEditor(model.getFileSystem(), entry.getPath());
                });
                getItems().add(0, executeInBackground);

                var execute = new MenuItem("Run in terminal");
                execute.setOnAction(event -> {
                    event.consume();
                    try {
                        ShellProcessControl pc = model.getFileSystem().getShell().orElseThrow();
                        pc.executeSimpleCommand(pc.getShellDialect().getMakeExecutableCommand(entry.getPath()));
                        var cmd = pc.command(entry.getPath()).prepareTerminalOpen();
                        TerminalHelper.open(FilenameUtils.getName(entry.getPath()), cmd);
                    } catch (Exception e) {
                        ErrorEvent.fromThrowable(e).handle();
                    }
                });
                getItems().add(0, execute);
            }

            var edit = new MenuItem("Edit");
            edit.setOnAction(event -> {
                event.consume();
                ExternalEditor.get().openInEditor(model.getFileSystem(), entry.getPath());
            });
            getItems().add(0, edit);
        }
    }
}
