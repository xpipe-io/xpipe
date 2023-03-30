package io.xpipe.app.browser;

import io.xpipe.app.util.BusyProperty;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.store.FileSystem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Value;
import org.apache.commons.io.FileUtils;

import java.nio.file.Path;
import java.util.List;

@Value
public class LocalFileTransferStage {

    private static final Path TEMP =
            FileUtils.getTempDirectory().toPath().resolve("xpipe").resolve("download");

    @Value
    public static class Item {
        FileSystem.FileEntry fileEntry;
        Path localFile;
        BooleanProperty finishedDownload = new SimpleBooleanProperty();
    }

    ObservableList<Item> items = FXCollections.observableArrayList();
    BooleanProperty downloading = new SimpleBooleanProperty();

    public void drop(List<FileSystem.FileEntry> entries) {
        entries.forEach(entry -> {
            Path file = TEMP.resolve(FileNames.getFileName(entry.getPath()));
            var item = new Item(entry, file);
            items.add(item);
            ThreadHelper.runFailableAsync(() -> {
                FileUtils.forceMkdirParent(TEMP.toFile());
                try (var b = new BusyProperty(downloading)) {
                    FileSystemHelper.dropFilesInto(FileSystemHelper.getLocal(TEMP),List.of(entry), false);
                }
                item.finishedDownload.set(true);
            });
        });
    }
}
