package io.xpipe.app.browser;

import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.FileBridge;
import io.xpipe.app.util.FileOpener;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FileNames;

import java.io.OutputStream;

public class BrowserFileOpener {

    public static void openWithAnyApplication(OpenFileSystemModel model, FileEntry entry) {
        var file = entry.getPath();
        var key = entry.getPath().hashCode() + entry.getFileSystem().hashCode();
        FileBridge.get()
                .openIO(
                        FileNames.getFileName(file),
                        key,
                        new BooleanScope(model.getBusy()).exclusive(),
                        () -> {
                            return entry.getFileSystem().openInput(file);
                        },
                        (size) -> {
                            if (model.isClosed()) {
                                return OutputStream.nullOutputStream();
                            }

                            return entry.getFileSystem().openOutput(file, size);
                        },
                        s -> FileOpener.openWithAnyApplication(s));
    }

    public static void openInDefaultApplication(OpenFileSystemModel model, FileEntry entry) {
        var file = entry.getPath();
        var key = entry.getPath().hashCode() + entry.getFileSystem().hashCode();
        FileBridge.get()
                .openIO(
                        FileNames.getFileName(file),
                        key,
                        new BooleanScope(model.getBusy()).exclusive(),
                        () -> {
                            return entry.getFileSystem().openInput(file);
                        },
                        (size) -> {
                            if (model.isClosed()) {
                                return OutputStream.nullOutputStream();
                            }

                            return entry.getFileSystem().openOutput(file, size);
                        },
                        s -> FileOpener.openInDefaultApplication(s));
    }

    public static void openInTextEditor(OpenFileSystemModel model, FileEntry entry) {
        var editor = AppPrefs.get().externalEditor().getValue();
        if (editor == null) {
            return;
        }

        var file = entry.getPath();
        var key = entry.getPath().hashCode() + entry.getFileSystem().hashCode();
        FileBridge.get()
                .openIO(
                        FileNames.getFileName(file),
                        key,
                        new BooleanScope(model.getBusy()).exclusive(),
                        () -> {
                            return entry.getFileSystem().openInput(file);
                        },
                        (size) -> {
                            if (model.isClosed()) {
                                return OutputStream.nullOutputStream();
                            }

                            return entry.getFileSystem().openOutput(file, size);
                        },
                        FileOpener::openInTextEditor);
    }
}
