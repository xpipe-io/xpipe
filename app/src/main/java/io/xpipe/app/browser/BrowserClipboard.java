package io.xpipe.app.browser;

import io.xpipe.app.browser.file.FileSystemHelper;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.ProcessControlProvider;
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
import java.util.stream.Collectors;

public class BrowserClipboard {

    public static final Property<Instance> currentCopyClipboard = new SimpleObjectProperty<>();
    public static Instance currentDragClipboard;

    static {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .addFlavorListener(e -> ThreadHelper.runFailableAsync(new FailableRunnable<>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public void run() {
                        Clipboard clipboard = (Clipboard) e.getSource();
                        try {
                            if (!clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                                return;
                            }

                            List<File> data = (List<File>) clipboard.getData(DataFlavor.javaFileListFlavor);
                            var files =
                                    data.stream().map(string -> string.toPath()).toList();
                            if (files.size() == 0) {
                                return;
                            }

                            var entries = new ArrayList<FileSystem.FileEntry>();
                            for (Path file : files) {
                                entries.add(FileSystemHelper.getLocal(file));
                            }

                            currentCopyClipboard.setValue(new Instance(UUID.randomUUID(), null, entries));
                        } catch (Exception e) {
                            ErrorEvent.fromThrowable(e).expected().omit().handle();
                        }
                    }
                }));
    }

    @SneakyThrows
    public static ClipboardContent startDrag(FileSystem.FileEntry base, List<FileSystem.FileEntry> selected) {
        if (selected.isEmpty()) {
            return null;
        }

        var content = new ClipboardContent();
        var id = UUID.randomUUID();
        currentDragClipboard = new Instance(id, base, new ArrayList<>(selected));
        content.putString(currentDragClipboard.toClipboardString());
        return content;
    }

    @SneakyThrows
    public static void startCopy(FileSystem.FileEntry base, List<FileSystem.FileEntry> selected) {
        if (selected.isEmpty()) {
            currentCopyClipboard.setValue(null);
            return;
        }

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

        if (currentDragClipboard == null) {
            return null;
        }

        try {
            var s = dragboard.getString();
            if (s != null && s.equals(currentDragClipboard.toClipboardString())) {
                var current = currentDragClipboard;
                currentDragClipboard = null;
                return current;
            }
        } catch (Exception ex) {
            return null;
        }

        return null;
    }

    @Value
    public static class Instance {
        UUID uuid;
        FileSystem.FileEntry baseDirectory;
        List<FileSystem.FileEntry> entries;

        public String toClipboardString() {
            return entries.stream()
                    .map(fileEntry -> "\"" + fileEntry.getPath() + "\"")
                    .collect(Collectors.joining(ProcessControlProvider.get()
                            .getEffectiveLocalDialect()
                            .getNewLine()
                            .getNewLineString()));
        }
    }
}
