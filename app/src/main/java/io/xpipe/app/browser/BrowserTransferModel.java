package io.xpipe.app.browser;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileTransferMode;
import io.xpipe.app.browser.file.BrowserFileTransferOperation;
import io.xpipe.app.browser.file.LocalFileSystem;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ShellTemp;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.Value;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Value
public class BrowserTransferModel {

    private static final Path TEMP = ShellTemp.getLocalTempDataDirectory("download");

    ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        t.setName("file downloader");
        return t;
    });
    BrowserSessionModel browserSessionModel;
    ObservableList<Item> items = FXCollections.observableArrayList();
    BooleanProperty downloading = new SimpleBooleanProperty();
    BooleanProperty allDownloaded = new SimpleBooleanProperty();

    private void cleanDirectory() {
        if (!Files.isDirectory(TEMP)) {
            return;
        }

        try (var ls = Files.list(TEMP)) {
            var list = ls.toList();
            for (Path path : list) {
                FileUtils.forceDelete(path.toFile());
            }
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    public void clear() {
        executor.submit(() -> {
            cleanDirectory();
            items.clear();
        });
    }

    public void drop(OpenFileSystemModel model, List<BrowserEntry> entries) {
        entries.forEach(entry -> {
            var name = entry.getFileName();
            if (items.stream().anyMatch(item -> item.getName().equals(name))) {
                return;
            }

            Path file = TEMP.resolve(name);
            var item = new Item(model, name, entry, file);
            items.add(item);
            allDownloaded.set(false);
        });
    }

    public void dropLocal(List<File> entries) {
        if (entries.isEmpty()) {
            return;
        }

        var empty = items.isEmpty();
        try {
            var paths = entries.stream().map(File::toPath).filter(Files::exists).toList();
            for (Path path : paths) {
                var entry = LocalFileSystem.getLocalBrowserEntry(path);
                var name = entry.getFileName();
                if (items.stream().anyMatch(item -> item.getName().equals(name))) {
                    return;
                }

                var item = new Item(null, name, entry, path);
                item.progress.setValue(BrowserTransferProgress.finished(
                        entry.getFileName(), entry.getRawFileEntry().getSize()));
                items.add(item);
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }
        if (empty) {
            allDownloaded.set(true);
        }
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
                if (item.downloadFinished().get()) {
                    continue;
                }

                if (item.getOpenFileSystemModel() != null
                        && item.getOpenFileSystemModel().isClosed()) {
                    continue;
                }

                try {
                    try (var ignored = new BooleanScope(downloading).start()) {
                        var op = new BrowserFileTransferOperation(
                                LocalFileSystem.getLocalFileEntry(TEMP),
                                List.of(item.getBrowserEntry().getRawFileEntry()),
                                BrowserFileTransferMode.COPY,
                                false,
                                progress -> {
                                    item.getProgress().setValue(progress);
                                    item.getOpenFileSystemModel().getProgress().setValue(progress);
                                });
                        op.execute();
                    }
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
        OpenFileSystemModel openFileSystemModel;
        String name;
        BrowserEntry browserEntry;
        Path localFile;
        Property<BrowserTransferProgress> progress;

        public Item(OpenFileSystemModel openFileSystemModel, String name, BrowserEntry browserEntry, Path localFile) {
            this.openFileSystemModel = openFileSystemModel;
            this.name = name;
            this.browserEntry = browserEntry;
            this.localFile = localFile;
            this.progress = new SimpleObjectProperty<>();
        }

        public ObservableBooleanValue downloadFinished() {
            return Bindings.createBooleanBinding(
                    () -> {
                        return progress.getValue() != null
                                && progress.getValue().done();
                    },
                    progress);
        }
    }
}
