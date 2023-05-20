package io.xpipe.app.browser;

import io.xpipe.core.store.FileSystem;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import lombok.SneakyThrows;
import lombok.Value;

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

    public static Property<Instance> currentCopyClipboard = new SimpleObjectProperty<>();
    public static Instance currentDragClipboard;

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
        var current = currentCopyClipboard;
        return current.getValue();
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
