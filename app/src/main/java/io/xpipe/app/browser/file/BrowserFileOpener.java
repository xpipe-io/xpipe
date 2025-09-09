package io.xpipe.app.browser.file;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.ConnectionFileSystem;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ElevationFunction;
import io.xpipe.app.process.ProcessOutputException;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.FileBridge;
import io.xpipe.app.util.FileOpener;
import io.xpipe.app.ext.FileInfo;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import lombok.SneakyThrows;

import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BrowserFileOpener {

    private static BrowserFileOutput openFileOutput(BrowserFileSystemTabModel model, FileEntry file, long totalBytes)
            throws Exception {
        if (model.isClosed()) {
            return BrowserFileOutput.none();
        }

        if (totalBytes == 0) {
            var existingSize = model.getFileSystem().getFileSize(file.getPath());
            if (existingSize != 0) {
                var blank = AppDialog.confirm(
                        "fileWriteBlankTitle", AppI18n.observable("fileWriteBlankContent", file.getPath()));
                if (!blank) {
                    return BrowserFileOutput.none();
                }
            }
        }

        var defOutput = createFileOutput(model, file, totalBytes, false);
        if (model.getFileSystem().getShell().isEmpty()) {
            return defOutput;
        }

        var sc = model.getFileSystem().getShell().orElseThrow();
        var requiresSudo =
                sc.getOsType() != OsType.WINDOWS && requiresSudo(model, (FileInfo.Unix) file.getInfo(), file.getPath());

        if (!requiresSudo) {
            return defOutput;
        }

        var elevate = AppDialog.confirm("fileWriteSudo");
        if (!elevate) {
            return defOutput;
        }

        var rootOutput = createFileOutput(model, file, totalBytes, true);
        return rootOutput;
    }

    private static boolean requiresSudo(BrowserFileSystemTabModel model, FileInfo.Unix info, FilePath filePath)
            throws Exception {
        if (model.getFileSystem().getShell().isEmpty() || model.getCache() == null) {
            return false;
        }

        if (model.getCache().isRoot()) {
            return false;
        }

        if (info != null) {
            var otherWrite = info.getPermissions().charAt(7) == 'w';
            if (otherWrite) {
                return false;
            }

            var userOwned = info.getUid() != null
                            && model.getCache().getUidForUser(model.getCache().getUsername()) == info.getUid()
                    || info.getUser() != null && model.getCache().getUsername().equals(info.getUser());
            var userWrite = info.getPermissions().charAt(1) == 'w';
            if (userOwned && userWrite) {
                return false;
            }
        }

        var test = model.getFileSystem()
                .getShell()
                .orElseThrow()
                .command(CommandBuilder.of().add("test", "-w").addFile(filePath))
                .executeAndCheck();
        return !test;
    }

    private static BrowserFileOutput createFileOutput(
            BrowserFileSystemTabModel model, FileEntry file, long totalBytes, boolean elevate) throws Exception {
        var shell = model.getFileSystem().getShell();
        var sc = shell.isEmpty()
                ? null
                : elevate
                        ? shell.orElseThrow()
                                .identicalDialectSubShell()
                                .elevated(ElevationFunction.elevated(null))
                                .start()
                        : model.getFileSystem().getShell().orElseThrow().start();
        var fs = elevate ? new ConnectionFileSystem(sc) : model.getFileSystem();
        var checkSudoersFile = shell.isPresent() && file.getPath().startsWith("/etc/sudo");
        var output = new BrowserFileOutput() {

            @Override
            public Optional<DataStoreEntry> target() {
                return Optional.of(model.getEntry().get());
            }

            @Override
            public boolean hasOutput() {
                return true;
            }

            @Override
            public OutputStream open() throws Exception {
                try {
                    return fs.openOutput(file.getPath(), totalBytes);
                } catch (Exception ex) {
                    if (elevate) {
                        fs.close();
                    }
                    throw ex;
                }
            }

            @Override
            public void beforeTransfer() throws Exception {
                if (checkSudoersFile) {
                    fs.copy(file.getPath(), sc.getSystemTemporaryDirectory().join(file.getName()));
                }
            }

            @Override
            public void onFinish() throws Exception {
                if (checkSudoersFile) {
                    if (sc.view().findProgram("visudo").isPresent()) {
                        try {
                            sc.command(CommandBuilder.of()
                                            .add("visudo", "-c", "-f")
                                            .addFile(file.getPath()))
                                    .execute();
                        } catch (ProcessOutputException ex) {
                            ErrorEventFactory.fromThrowable(ex).expected().handle();
                            fs.copy(sc.getSystemTemporaryDirectory().join(file.getName()), file.getPath());
                        }
                    }
                }

                if (elevate) {
                    fs.close();
                }

                model.refreshFileEntriesSync(List.of(file));
            }
        };
        return output;
    }

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
                        () -> {
                            return entry.getFileSystem().openInput(file);
                        },
                        (size) -> {
                            if (model.isClosed()) {
                                return BrowserFileOutput.none();
                            }

                            return new BrowserFileOutput() {
                                @Override
                                public Optional<DataStoreEntry> target() {
                                    return Optional.of(model.getEntry().get());
                                }

                                @Override
                                public boolean hasOutput() {
                                    return true;
                                }

                                @Override
                                public OutputStream open() throws Exception {
                                    return entry.getFileSystem().openOutput(file, size);
                                }

                                @Override
                                public void beforeTransfer() {}

                                @Override
                                public void onFinish() {
                                    model.refreshFileEntriesSync(List.of(entry));
                                }
                            };
                        },
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
                        () -> {
                            return entry.getFileSystem().openInput(file);
                        },
                        (size) -> {
                            if (model.isClosed()) {
                                return BrowserFileOutput.none();
                            }

                            return new BrowserFileOutput() {
                                @Override
                                public Optional<DataStoreEntry> target() {
                                    return Optional.of(model.getEntry().get());
                                }

                                @Override
                                public boolean hasOutput() {
                                    return true;
                                }

                                @Override
                                public OutputStream open() throws Exception {
                                    return entry.getFileSystem().openOutput(file, size);
                                }

                                @Override
                                public void beforeTransfer() {}

                                @Override
                                public void onFinish() {
                                    model.refreshFileEntriesSync(List.of(entry));
                                }
                            };
                        },
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

        var file = entry.getPath();
        var key = calculateKey(model, entry);
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
