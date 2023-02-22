package io.xpipe.app.browser;

import io.xpipe.core.store.FileSystem;
import io.xpipe.core.util.XPipeTempDirectory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import lombok.SneakyThrows;
import lombok.Value;

import java.nio.file.Files;
import java.util.*;

public class FileBrowserClipboard {

    @Value
    public static class Instance {
        UUID uuid;
        List<FileSystem.FileEntry> entries;
    }

    public static Map<UUID, List<FileSystem.FileEntry>> CLIPBOARD = new HashMap<>();
    public static Instance currentCopyClipboard;
    public static Instance currentDragClipboard;

    @SneakyThrows
    public static ClipboardContent startDrag(List<FileSystem.FileEntry> selected) {
        var content = new ClipboardContent();
        var idea = UUID.randomUUID();
        var file = XPipeTempDirectory.getLocal().resolve(idea.toString());
        Files.createFile(file);
        currentDragClipboard = new Instance(idea, selected);
        content.putFiles(List.of(file.toFile()));
        return content;
    }

    @SneakyThrows
    public static void startCopy(List<FileSystem.FileEntry> selected) {
        var idea = UUID.randomUUID();
        currentCopyClipboard = new Instance(idea, new ArrayList<>(selected));
    }

    public static Instance retrieveCopy() {
        var current = currentCopyClipboard;
        return current;
    }

    public static Instance retrieveDrag(Dragboard dragboard) {
        if (dragboard.getFiles().size() != 1) {
            return null;
        }

        var idea = UUID.fromString(dragboard.getFiles().get(0).toPath().getFileName().toString());
        if (idea.equals(currentDragClipboard.uuid)) {
            var current = currentDragClipboard;
            currentDragClipboard = null;
            return current;
        }

        return null;
    }
}
