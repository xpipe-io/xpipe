package io.xpipe.app.browser.file;

import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.FileBridge;
import io.xpipe.app.util.FileOpener;
import io.xpipe.core.process.ElevationFunction;
import io.xpipe.core.process.OsType;
import io.xpipe.app.ext.ConnectionFileSystem;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FileInfo;
import io.xpipe.core.store.FileNames;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class BrowserFileOpener {

    private static OutputStream openFileOutput(BrowserFileSystemTabModel model, FileEntry file, long totalBytes)
            throws Exception {
        var fileSystem = model.getFileSystem();
        if (model.isClosed() || fileSystem.getShell().isEmpty()) {
            return OutputStream.nullOutputStream();
        }

        var sc = fileSystem.getShell().get();
        if (sc.getOsType() == OsType.WINDOWS) {
            return fileSystem.openOutput(file.getPath(), totalBytes);
        }

        var info = (FileInfo.Unix) file.getInfo();
        var zero = Integer.valueOf(0);
        var otherWrite = info.getPermissions().charAt(7) == 'w';
        var requiresRoot = zero.equals(info.getUid()) && zero.equals(info.getGid()) && !otherWrite;
        if (!requiresRoot || model.getCache().isRoot()) {
            return fileSystem.openOutput(file.getPath(), totalBytes);
        }

        var elevate = AppWindowHelper.showConfirmationAlert(
                "app.fileWriteSudoTitle", "app.fileWriteSudoHeader", "app.fileWriteSudoContent");
        if (!elevate) {
            return fileSystem.openOutput(file.getPath(), totalBytes);
        }

        var rootSc = sc.identicalSubShell()
                .elevated(ElevationFunction.elevated(null))
                .start();
        var rootFs = new ConnectionFileSystem(rootSc);
        try {
            return new FilterOutputStream(rootFs.openOutput(file.getPath(), totalBytes)) {
                @Override
                public void close() throws IOException {
                    super.close();
                    rootFs.close();
                }
            };
        } catch (Exception ex) {
            rootFs.close();
            throw ex;
        }
    }

    private static int calculateKey(FileEntry entry) {
        return Objects.hash(entry.getPath(), entry.getFileSystem(), entry.getKind(), entry.getInfo());
    }

    public static void openWithAnyApplication(BrowserFileSystemTabModel model, FileEntry entry) {
        var file = entry.getPath();
        var key = calculateKey(entry);
        FileBridge.get()
                .openIO(
                        file.getFileName(),
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

    public static void openInDefaultApplication(BrowserFileSystemTabModel model, FileEntry entry) {
        var file = entry.getPath();
        var key = calculateKey(entry);
        FileBridge.get()
                .openIO(
                        file.getFileName(),
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

    public static void openInTextEditor(BrowserFileSystemTabModel model, FileEntry entry) {
        var editor = AppPrefs.get().externalEditor().getValue();
        if (editor == null) {
            return;
        }

        var file = entry.getPath();
        var key = calculateKey(entry);
        FileBridge.get()
                .openIO(
                        file.getFileName(),
                        key,
                        new BooleanScope(model.getBusy()).exclusive(),
                        () -> {
                            return entry.getFileSystem().openInput(file);
                        },
                        (size) -> {
                            return openFileOutput(model, entry, size);
                        },
                        FileOpener::openInTextEditor);
    }
}
