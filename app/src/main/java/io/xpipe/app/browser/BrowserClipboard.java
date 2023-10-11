package io.xpipe.app.browser;

import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.util.FailableRunnable;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import lombok.SneakyThrows;
import lombok.Value;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BrowserClipboard {

    @Value
    public static class Instance {
        UUID uuid;
        FileSystem.FileEntry baseDirectory;
        List<FileSystem.FileEntry> entries;
    }

    public static final Property<Instance> currentCopyClipboard = new SimpleObjectProperty<>();
    public static Instance currentDragClipboard;

    static {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .addFlavorListener(e -> ThreadHelper.runFailableAsync(new FailableRunnable<>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public void run() throws Throwable {
                        Clipboard clipboard = (Clipboard) e.getSource();
                        try {
                            if (!clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                                return;
                            }

                            List<File> data = (List<File>) clipboard.getData(DataFlavor.javaFileListFlavor);
                            var files = data.stream().map(string -> string.toPath()).toList();
                            if (files.size() == 0) {
                                return;
                            }

                            var entries = new ArrayList<FileSystem.FileEntry>();
                            for (Path file : files) {
                                entries.add(FileSystemHelper.getLocal(file));
                            }

                            currentCopyClipboard.setValue(new Instance(UUID.randomUUID(), null, entries));
                        } catch (IllegalStateException ignored) {
                        }
                    }
                }));
    }

    @SneakyThrows
    public static ClipboardContent startDrag(FileSystem.FileEntry base, List<FileSystem.FileEntry> selected) {
        var content = new ClipboardContent();
        var idea = UUID.randomUUID();
        currentDragClipboard = new Instance(idea, base, new ArrayList<>(selected));
        content.putString(idea.toString());
        return content;
    }

    @SneakyThrows
    public static void startCopy(FileSystem.FileEntry base, List<FileSystem.FileEntry> selected) {
        var id = UUID.randomUUID();
        currentCopyClipboard.setValue(new Instance(id, base, new ArrayList<>(selected)));
    }

    public static Instance retrieveCopy() {
        return currentCopyClipboard.getValue();
    }

    public static Instance retrieveDrag(Dragboard dragboard) {
        if (dragboard.getString() == null) {
            return null;
        }

        try {
            var idea = UUID.fromString(dragboard.getString());
            if (idea.equals(currentDragClipboard.uuid)) {
                var current = currentDragClipboard;
                currentDragClipboard = null;
                return current;
            }
        } catch (Exception ex) {
            return null;
        }

        return null;
    }
}
