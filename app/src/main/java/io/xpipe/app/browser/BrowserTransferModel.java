package io.xpipe.app.browser;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FileSystem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Value;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Value
public class BrowserTransferModel {

    private static final Path TEMP = FileUtils.getTempDirectory().toPath().resolve("xpipe").resolve("download");

    ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        t.setName("file downloader");
        return t;
    });
    BrowserModel browserModel;
    ObservableList<Item> items = FXCollections.observableArrayList();
    BooleanProperty downloading = new SimpleBooleanProperty();
    BooleanProperty allDownloaded = new SimpleBooleanProperty();

    public void clear() {
        try {
            FileUtils.deleteDirectory(TEMP.toFile());
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).handle();
        }
        items.clear();
    }

    public void drop(List<FileSystem.FileEntry> entries) {
        entries.forEach(entry -> {
            var name = FileNames.getFileName(entry.getPath());
            if (items.stream().anyMatch(item -> item.getName().equals(name))) {
                return;
            }

            Path file = TEMP.resolve(name);
            var item = new Item(name, entry, file);
            items.add(item);
            allDownloaded.set(false);
        });
    }

    public void download() {
        executor.submit(() -> {
            try {
                FileUtils.forceMkdir(TEMP.toFile());
            } catch (IOException e) {
                ErrorEvent.fromThrowable(e).handle();
                return;
            }

            for (Item item : new ArrayList<>(items)) {
                if (item.getFinishedDownload().get()) {
                    continue;
                }

                try {
                    try (var b = new BooleanScope(downloading).start()) {
                        FileSystemHelper.dropFilesInto(FileSystemHelper.getLocal(TEMP), List.of(item.getFileEntry()), true);
                    }
                    item.finishedDownload.set(true);
                } catch (Throwable t) {
                    ErrorEvent.fromThrowable(t).handle();
                    items.remove(item);
                }
            }
            allDownloaded.set(true);
        });
    }

    @Value
    public static class Item {
        String name;
        FileSystem.FileEntry fileEntry;
        Path localFile;
        BooleanProperty finishedDownload = new SimpleBooleanProperty();
    }
}
