package io.xpipe.app.browser.file;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.ConnectionFileSystem;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.FileBridge;
import io.xpipe.app.util.FileOpener;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ElevationFunction;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FileInfo;
import io.xpipe.core.store.FilePath;

import lombok.SneakyThrows;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;

public class BrowserFileOpener {

    private static BrowserFileOutput openFileOutput(BrowserFileSystemTabModel model, FileEntry file, long totalBytes)
            throws Exception {
        var fileSystem = model.getFileSystem();
        if (model.isClosed() || fileSystem.getShell().isEmpty()) {
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

        var defOutput = new BrowserFileOutput() {

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
                return fileSystem.openOutput(file.getPath(), totalBytes);
            }
        };

        var sc = fileSystem.getShell().get();
        if (sc.getOsType() == OsType.WINDOWS) {
            return defOutput;
        }

        var info = (FileInfo.Unix) file.getInfo();
        var requiresSudo = requiresSudo(model, info, file.getPath());
        if (!requiresSudo) {
            return defOutput;
        }

        var elevate = AppDialog.confirm("fileWriteSudo");
        if (!elevate) {
            return defOutput;
        }

        var rootSc = sc.identicalDialectSubShell()
                .elevated(ElevationFunction.elevated(null))
                .start();
        var rootFs = new ConnectionFileSystem(rootSc);
        var rootOutput = new BrowserFileOutput() {

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
                    return new FilterOutputStream(rootFs.openOutput(file.getPath(), totalBytes)) {
                        @Override
                        public void close() throws IOException {
                            try {
                                super.close();
                            } finally {
                                rootFs.close();
                            }
                        }
                    };
                } catch (Exception ex) {
                    rootFs.close();
                    throw ex;
                }
            }
        };
        return rootOutput;
    }

    private static boolean requiresSudo(BrowserFileSystemTabModel model, FileInfo.Unix info, FilePath filePath)
            throws Exception {
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

    @SneakyThrows
    private static int calculateKey(BrowserFileSystemTabModel model, FileEntry entry) {
        // Use different key for empty / non-empty files to prevent any issues from blanked files when transfer fails
        var empty = model.getFileSystem().getFileSize(entry.getPath()) == 0;
        return Objects.hash(entry.getPath(), entry.getFileSystem(), entry.getKind(), entry.getInfo(), empty);
    }

    public static void openWithAnyApplication(BrowserFileSystemTabModel model, FileEntry entry) {
        if (model.getFileSystem().getShell().orElseThrow().isLocal()) {
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
                                public boolean hasOutput() {
                                    return true;
                                }

                                @Override
                                public Optional<DataStoreEntry> target() {
                                    return Optional.of(model.getEntry().get());
                                }

                                @Override
                                public OutputStream open() throws Exception {
                                    return entry.getFileSystem().openOutput(file, size);
                                }
                            };
                        },
                        s -> FileOpener.openWithAnyApplication(s));
    }

    public static void openInDefaultApplication(BrowserFileSystemTabModel model, FileEntry entry) {
        if (model.getFileSystem().getShell().orElseThrow().isLocal()) {
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
                                public boolean hasOutput() {
                                    return true;
                                }

                                @Override
                                public Optional<DataStoreEntry> target() {
                                    return Optional.of(model.getEntry().get());
                                }

                                @Override
                                public OutputStream open() throws Exception {
                                    return entry.getFileSystem().openOutput(file, size);
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
        if (model.getFileSystem().getShell().orElseThrow().isLocal()) {
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
