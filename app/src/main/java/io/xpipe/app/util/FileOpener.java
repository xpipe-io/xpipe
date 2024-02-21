package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.CommandControl;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FileSystem;
import lombok.SneakyThrows;

import java.io.FilterInputStream;
import java.nio.file.Path;
import java.util.function.Consumer;

public class FileOpener {

    public static void openWithAnyApplication(FileSystem.FileEntry entry) {
        var file = entry.getPath();
        var key = entry.getPath().hashCode() + entry.getFileSystem().hashCode();
        FileBridge.get()
                .openIO(
                        FileNames.getFileName(file),
                        key,
                        () -> {
                            return entry.getFileSystem().openInput(file);
                        },
                        (size) -> entry.getFileSystem().openOutput(file, size),
                        s -> openWithAnyApplication(s));
    }

    public static void openInDefaultApplication(FileSystem.FileEntry entry) {
        var file = entry.getPath();
        var key = entry.getPath().hashCode() + entry.getFileSystem().hashCode();
        FileBridge.get()
                .openIO(
                        FileNames.getFileName(file),
                        key,
                        () -> {
                            return entry.getFileSystem().openInput(file);
                        },
                        (size) -> entry.getFileSystem().openOutput(file, size),
                        s -> openInDefaultApplication(s));
    }

    public static void openInTextEditor(FileSystem.FileEntry entry) {
        var editor = AppPrefs.get().externalEditor().getValue();
        if (editor == null || !editor.isSelectable()) {
            return;
        }

        var file = entry.getPath();
        var key = entry.getPath().hashCode() + entry.getFileSystem().hashCode();
        FileBridge.get()
                .openIO(
                        FileNames.getFileName(file),
                        key,
                        () -> {
                            return entry.getFileSystem().openInput(file);
                        },
                        (size) -> entry.getFileSystem().openOutput(file, size),
                        FileOpener::openInTextEditor);
    }

    public static void openInTextEditor(String localFile) {
        var editor = AppPrefs.get().externalEditor().getValue();
        if (editor == null) {
            return;
        }

        try {
            editor.launch(Path.of(localFile).toRealPath());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).expected().handle();
        }
    }

    public static void openWithAnyApplication(String localFile) {
        try {
            switch (OsType.getLocal()) {
                case OsType.Windows windows -> {
                    var cmd = CommandBuilder.of().add("rundll32.exe", "shell32.dll,OpenAs_RunDLL", localFile);
                    LocalShell.getShell().executeSimpleCommand(cmd);
                }
                case OsType.Linux linux -> {
                    LocalShell.getShell()
                            .executeSimpleCommand("mimeopen -a "
                                    + LocalShell.getShell().getShellDialect().fileArgument(localFile));
                }
                case OsType.MacOs macOs -> {
                    throw new UnsupportedOperationException();
                }
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e)
                    .description("Unable to open file " + localFile)
                    .handle();
        }
    }

    public static void openInDefaultApplication(String localFile) {
        try (var pc = LocalShell.getShell().start()) {
            if (pc.getOsType().equals(OsType.WINDOWS)) {
                pc.executeSimpleCommand("start \"\" \"" + localFile + "\"");
            } else if (pc.getOsType().equals(OsType.LINUX)) {
                pc.executeSimpleCommand("xdg-open \"" + localFile + "\"");
            } else {
                pc.executeSimpleCommand("open \"" + localFile + "\"");
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e)
                    .description("Unable to open file " + localFile)
                    .handle();
        }
    }

    public static void openReadOnlyString(String input) {
        FileBridge.get().openReadOnlyString(input, s -> openInTextEditor(s));
    }

    public static void openString(String keyName, Object key, String input, Consumer<String> output) {
        FileBridge.get().openString(keyName, key, input, output, file -> openInTextEditor(file));
    }

    public static void openCommandOutput(String keyName, Object key, CommandControl cc) {
        FileBridge.get()
                .openIO(
                        keyName,
                        key,
                        () -> new FilterInputStream(cc.getStdout()) {
                            @Override
                            @SneakyThrows
                            public void close() {
                                cc.close();
                            }
                        },
                        null,
                        file -> openInTextEditor(file));
    }
}
