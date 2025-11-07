package io.xpipe.app.browser.file;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.FileBridge;
import io.xpipe.app.util.FileOpener;
import io.xpipe.app.util.HumanReadableFormat;

import lombok.SneakyThrows;

import java.util.Objects;

public class BrowserFileOpener {

    @SneakyThrows
    private static int calculateKey(BrowserFileSystemTabModel model, FileEntry entry) {
        // Use different key for empty / non-empty files to prevent any issues from blanked files when transfer fails
        var empty = model.getFileSystem().getFileSize(entry.getPath()) == 0;
        return Objects.hash(entry.getPath(), entry.getFileSystem(), entry.getKind(), entry.getInfo(), empty);
    }

    public static void openWithAnyApplication(BrowserFileSystemTabModel model, FileEntry entry) {
        if (model.getFileSystem().getShell().isPresent()
                && model.getFileSystem().getShell().get().isLocal()) {
            FileOpener.openWithAnyApplication(entry.getPath().toString());
            return;
        }

        var file = entry.getPath();
        var key = calculateKey(model, entry);
        FileBridge.get()
                .openIO(
                        file.getFileName(),
                        key,
                        new BooleanScope(model.getBusy()).exclusive(),
                        () -> BrowserFileInput.openFileInput(model, entry),
                        (size) -> BrowserFileOutput.openFileOutput(model, entry, size),
                        s -> FileOpener.openWithAnyApplication(s));
    }

    public static void openInDefaultApplication(BrowserFileSystemTabModel model, FileEntry entry) {
        if (model.getFileSystem().getShell().isPresent()
                && model.getFileSystem().getShell().get().isLocal()) {
            FileOpener.openInDefaultApplication(entry.getPath().toString());
            return;
        }

        var file = entry.getPath();
        var key = calculateKey(model, entry);
        FileBridge.get()
                .openIO(
                        file.getFileName(),
                        key,
                        new BooleanScope(model.getBusy()).exclusive(),
                        () -> BrowserFileInput.openFileInput(model, entry),
                        (size) -> BrowserFileOutput.openFileOutput(model, entry, size),
                        s -> FileOpener.openInDefaultApplication(s));
    }

    public static void openInTextEditor(BrowserFileSystemTabModel model, FileEntry entry) {
        var editor = AppPrefs.get().externalEditor().getValue();
        if (editor == null) {
            return;
        }

        if (model.getFileSystem().getShell().isPresent()
                && model.getFileSystem().getShell().get().isLocal()) {
            FileOpener.openInTextEditor(entry.getPath().toString());
            return;
        }

        var size = entry.getFileSizeLong().orElse(0L);
        if (size > 1_000_000) {
            var confirm = AppDialog.confirm(
                    "largeFileWarningTitle",
                    AppI18n.observable("largeFileWarningContent", HumanReadableFormat.byteCount(size)));
            if (!confirm) {
                return;
            }
        }

        var file = entry.getPath();
        var key = calculateKey(model, entry);
        FileBridge.get()
                .openIO(
                        file.getFileName(),
                        key,
                        new BooleanScope(model.getBusy()).exclusive(),
                        () -> BrowserFileInput.openFileInput(model, entry),
                        (os) -> {
                            return BrowserFileOutput.openFileOutput(model, entry, os);
                        },
                        FileOpener::openInTextEditor);
    }
}
