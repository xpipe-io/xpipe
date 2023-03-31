package io.xpipe.app.browser;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.BusyProperty;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Value
public class LocalFileTransferStage {

    private static final Path TEMP =
            FileUtils.getTempDirectory().toPath().resolve("xpipe").resolve("download");

    ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        t.setName("file downloader");
        return t;
    });

    @Value
    public static class Item {
        String name;
        FileSystem.FileEntry fileEntry;
        Path localFile;
        BooleanProperty finishedDownload = new SimpleBooleanProperty();
    }

    ObservableList<Item> items = FXCollections.observableArrayList();
    BooleanProperty downloading = new SimpleBooleanProperty();

    public void drop(List<FileSystem.FileEntry> entries) {
        entries.forEach(entry -> {
            var name = FileNames.getFileName(entry.getPath());
            if (items.stream().anyMatch(item -> item.getName().equals(name))) {
                return;
            }

            Path file = TEMP.resolve(name);
            var item = new Item(name, entry, file);
            items.add(item);
            executor.submit(() -> {
                try {
                    FileUtils.forceMkdirParent(TEMP.toFile());
                    try (var b = new BusyProperty(downloading)) {
                        FileSystemHelper.dropFilesInto(FileSystemHelper.getLocal(TEMP), List.of(entry), false);
                    }
                    item.finishedDownload.set(true);
                } catch (Throwable t) {
                    ErrorEvent.fromThrowable(t).handle();
                }
            });
        });
    }
}
