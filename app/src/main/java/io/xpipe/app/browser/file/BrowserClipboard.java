package io.xpipe.app.browser.file;

import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.GlobalClipboard;
import io.xpipe.app.platform.GlobalObjectProperty;

import javafx.beans.property.Property;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;

import lombok.SneakyThrows;
import lombok.Value;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BrowserClipboard {

    public static final Property<Instance> currentCopyClipboard = new GlobalObjectProperty<>();
    private static final DataFormat DATA_FORMAT = new DataFormat("application/xpipe-file-list");
    public static Instance currentDragClipboard;

    static {
        GlobalClipboard.addListener(new Consumer<>() {
            @Override
            @SuppressWarnings("unchecked")
            public void accept(Clipboard clipboard) {
                try {
                    if (!clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                        return;
                    }

                    List<File> data = (List<File>) clipboard.getData(DataFlavor.javaFileListFlavor);
                    // Sometimes file data can contain invalid chars. Why?
                    var files = data.stream()
                            .filter(file -> file.toString().chars().noneMatch(value -> Character.isISOControl(value)))
                            .map(f -> f.toPath())
                            .toList();
                    if (files.size() == 0) {
                        return;
                    }

                    var entries = new ArrayList<BrowserEntry>();
                    for (Path file : files) {
                        entries.add(BrowserLocalFileSystem.getLocalBrowserEntry(file));
                    }

                    currentCopyClipboard.setValue(
                            new Instance(UUID.randomUUID(), null, entries, BrowserFileTransferMode.COPY));
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).expected().omit().handle();
                }
            }
        });
    }

    @SneakyThrows
    public static ClipboardContent startDrag(
            FileEntry base, List<BrowserEntry> selected, BrowserFileTransferMode mode) {
        if (selected.isEmpty()) {
            return null;
        }

        var content = new ClipboardContent();
        var id = UUID.randomUUID();
        currentDragClipboard = new Instance(id, base, new ArrayList<>(selected), mode);
        content.put(DATA_FORMAT, currentDragClipboard.toClipboardString());
        return content;
    }

    @SneakyThrows
    public static void startCopy(FileEntry base, List<BrowserEntry> selected) {
        if (selected.isEmpty()) {
            currentCopyClipboard.setValue(null);
            return;
        }

        var id = UUID.randomUUID();
        currentCopyClipboard.setValue(new Instance(id, base, new ArrayList<>(selected), BrowserFileTransferMode.COPY));
    }

    public static Instance retrieveCopy() {
        return currentCopyClipboard.getValue();
    }

    public static Instance retrieveDrag(Dragboard dragboard) {
        if (currentDragClipboard == null) {
            return null;
        }

        try {
            var s = dragboard.getContent(DATA_FORMAT);
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
        FileEntry baseDirectory;
        List<BrowserEntry> entries;
        BrowserFileTransferMode mode;

        public String toClipboardString() {
            return entries.stream()
                    .map(fileEntry -> "\"" + fileEntry.getRawFileEntry().getPath() + "\"")
                    .collect(Collectors.joining(ProcessControlProvider.get()
                            .getEffectiveLocalDialect()
                            .getNewLine()
                            .getNewLineString()));
        }
    }
}
