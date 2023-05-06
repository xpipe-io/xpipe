/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import io.xpipe.app.comp.source.GuiDsCreatorMultiStep;
import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.util.FileOpener;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.TerminalHelper;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileSystem;
import javafx.beans.property.Property;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;

final class FileContextMenu extends ContextMenu {

    public boolean isExecutable(FileSystem.FileEntry e) {
        if (e.isDirectory()) {
            return false;
        }

        if (e.getExecutable() != null && e.getExecutable()) {
            return true;
        }

        var shell = e.getFileSystem().getShell();
        if (shell.isEmpty()) {
            return false;
        }

        var os = shell.get().getOsType();
        var ending = FilenameUtils.getExtension(e.getPath()).toLowerCase();
        if (os.equals(OsType.WINDOWS) && List.of("exe", "bat", "ps1", "cmd").contains(ending)) {
            return true;
        }

        if (List.of("sh", "command").contains(ending)) {
            return true;
        }

        return false;
    }

    private final OpenFileSystemModel model;
    private final FileSystem.FileEntry entry;
    private final Property<FileSystem.FileEntry> editing;

    public FileContextMenu(OpenFileSystemModel model, FileSystem.FileEntry entry, Property<FileSystem.FileEntry> editing) {
        super();
        this.model = model;
        this.entry = entry;
        this.editing = editing;
        createMenu();
    }

    private void createMenu() {
        if (entry.isDirectory()) {
            var terminal = new MenuItem("Open terminal");
            terminal.setOnAction(event -> {
                event.consume();
                model.openTerminalAsync(entry.getPath());
            });
            getItems().add(terminal);
        } else {
            if (isExecutable(entry)) {
                var execute = new MenuItem("Run in terminal");
                execute.setOnAction(event -> {
                    ThreadHelper.runFailableAsync(() -> {
                        ShellControl pc = model.getFileSystem().getShell().orElseThrow();
                        var e = pc.getShellDialect().getMakeExecutableCommand(entry.getPath());
                        if (e != null) {
                            pc.executeSimpleBooleanCommand(e);
                        }
                        var cmd = pc.command("\"" + entry.getPath() + "\"").prepareTerminalOpen();
                        TerminalHelper.open(FilenameUtils.getBaseName(entry.getPath()), cmd);
                    });
                    event.consume();
                });
                getItems().add(execute);

                var executeInBackground = new MenuItem("Run in background");
                executeInBackground.setOnAction(event -> {
                    ThreadHelper.runFailableAsync(() -> {
                        ShellControl pc = model.getFileSystem().getShell().orElseThrow();
                        var e = pc.getShellDialect().getMakeExecutableCommand(entry.getPath());
                        if (e != null) {
                            pc.executeSimpleBooleanCommand(e);
                        }
                        var cmd = ScriptHelper.createDetachCommand(pc, "\"" + entry.getPath() + "\"");
                        pc.executeSimpleBooleanCommand(cmd);
                    });
                    event.consume();
                });
                getItems().add(executeInBackground);
            } else {
                var open = new MenuItem("Open default");
                open.setOnAction(event -> {
                    ThreadHelper.runFailableAsync(() -> {
                        FileOpener.openInDefaultApplication(entry);
                    });
                    event.consume();
                });
                getItems().add(open);
            }

            var pipe = new MenuItem("Pipe");
            pipe.setOnAction(event -> {
                var store = new FileStore(model.getFileSystem().getStore(), entry.getPath());
                GuiDsCreatorMultiStep.showForStore(DataSourceProvider.Category.STREAM, store, null);
                event.consume();
            });
            // getItems().add(pipe);

            var edit = new MenuItem("Edit");
            edit.setOnAction(event -> {
                ThreadHelper.runAsync(() -> FileOpener.openInTextEditor(entry));
                event.consume();
            });
            getItems().add(edit);
        }

        getItems().add(new SeparatorMenuItem());

        {

            var copy = new MenuItem("Copy");
            copy.setOnAction(event -> {
                FileBrowserClipboard.startCopy(
                        model.getCurrentDirectory(), model.getFileList().getSelected());
                event.consume();
            });
            getItems().add(copy);

            var paste = new MenuItem("Paste");
            paste.setOnAction(event -> {
                var clipboard = FileBrowserClipboard.retrieveCopy();
                if (clipboard != null) {
                    var files = clipboard.getEntries();
                    var target = entry.isDirectory() ? entry : model.getCurrentDirectory();
                    model.dropFilesIntoAsync(target, files, true);
                }
                event.consume();
            });
            getItems().add(paste);
        }

        getItems().add(new SeparatorMenuItem());

        var copyName = new MenuItem("Copy name");
        copyName.setOnAction(event -> {
            var selection = new StringSelection(FileNames.getFileName(entry.getPath()));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            event.consume();
        });
        getItems().add(copyName);

        var copyPath = new MenuItem("Copy full path");
        copyPath.setOnAction(event -> {
            var selection = new StringSelection(entry.getPath());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            event.consume();
        });
        getItems().add(copyPath);

        var delete = new MenuItem("Delete");
        delete.setOnAction(event -> {
            model.deleteSelectionAsync();
            event.consume();
        });

        var rename = new MenuItem("Rename");
        rename.setOnAction(event -> {
            event.consume();
            editing.setValue(entry);
        });

        getItems().addAll(new SeparatorMenuItem(), rename, delete);
    }
}
