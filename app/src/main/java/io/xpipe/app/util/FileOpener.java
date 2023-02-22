package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.store.ShellStore;
import org.apache.commons.io.FilenameUtils;

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
                        FilenameUtils.getExtension(file),
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
                        FilenameUtils.getExtension(file),
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
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    public static void openInDefaultApplication(String file) {
        try (var pc = ShellStore.local().create().start()) {
            if (pc.getOsType().equals(OsType.WINDOWS)) {
                pc.executeSimpleCommand("\"" + file + "\"");
            } else if (pc.getOsType().equals(OsType.LINUX)) {
                pc.executeSimpleCommand("xdg-open \"" + file + "\"");
            } else {
                pc.executeSimpleCommand("open \"" + file + "\"");
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    public static void openString(String keyName, String fileType, Object key, String input, Consumer<String> output) {
        FileBridge.get().openString(keyName, fileType, key, input, output, file -> openInTextEditor(file));
    }
}
