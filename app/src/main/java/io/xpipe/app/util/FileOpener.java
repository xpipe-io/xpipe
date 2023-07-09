package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.CommandControl;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileSystem;
import lombok.SneakyThrows;

import java.io.FilterInputStream;
import java.nio.file.Path;
import java.util.function.Consumer;

public class FileOpener {

    public static void openInDefaultApplication(FileSystem.FileEntry entry) {
        var editor = AppPrefs.get().externalEditor().getValue();
        if (editor == null || !editor.isSelectable()) {
            return;
        }

        var file = entry.getPath();
        FileBridge.get()
                .openIO(
                        FileNames.getFileName(file),
                        file,
                        () -> {
                            return entry.getFileSystem().openInput(file);
                        },
                        () -> entry.getFileSystem().openOutput(file),
                        s -> openInDefaultApplication(s));
    }

    public static void openInTextEditor(FileSystem.FileEntry entry) {
        var editor = AppPrefs.get().externalEditor().getValue();
        if (editor == null || !editor.isSelectable()) {
            return;
        }

        var file = entry.getPath();
        FileBridge.get()
                .openIO(
                        FileNames.getFileName(file),
                        file,
                        () -> {
                            return entry.getFileSystem().openInput(file);
                        },
                        () -> entry.getFileSystem().openOutput(file),
                        FileOpener::openInTextEditor);
    }

    public static void openInTextEditor(String file) {
        var editor = AppPrefs.get().externalEditor().getValue();
        if (editor == null) {
            return;
        }

        try {
            editor.launch(Path.of(file).toRealPath());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e)
                    .description("Unable to launch editor " + editor.toTranslatedString()
                            + ".\nMaybe try to use a different editor in the settings.")
                    .handle();
        }
    }

    public static void openInDefaultApplication(String file) {
        try (var pc = LocalStore.getShell().start()) {
            if (pc.getOsType().equals(OsType.WINDOWS)) {
                pc.executeSimpleCommand("start \"\" \"" + file + "\"");
            } else if (pc.getOsType().equals(OsType.LINUX)) {
                pc.executeSimpleCommand("xdg-open \"" + file + "\"");
            } else {
                pc.executeSimpleCommand("open \"" + file + "\"");
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e)
                    .description("Unable to open file " + file)
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
