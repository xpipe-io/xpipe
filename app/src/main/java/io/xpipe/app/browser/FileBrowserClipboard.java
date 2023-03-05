package io.xpipe.app.browser;

import io.xpipe.core.store.FileSystem;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import lombok.SneakyThrows;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileBrowserClipboard {

    @Value
    public static class Instance {
        UUID uuid;
        List<FileSystem.FileEntry> entries;
    }

    public static Instance currentCopyClipboard;
    public static Instance currentDragClipboard;

    @SneakyThrows
    public static ClipboardContent startDrag(List<FileSystem.FileEntry> selected) {
        var content = new ClipboardContent();
        var idea = UUID.randomUUID();
        currentDragClipboard = new Instance(idea, selected);
        content.putString(idea.toString());
        return content;
    }

    @SneakyThrows
    public static void startCopy(List<FileSystem.FileEntry> selected) {
        var id = UUID.randomUUID();
        currentCopyClipboard = new Instance(id, new ArrayList<>(selected));
    }

    public static Instance retrieveCopy() {
        var current = currentCopyClipboard;
        return current;
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
