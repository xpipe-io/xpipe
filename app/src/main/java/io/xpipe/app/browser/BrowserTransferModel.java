package io.xpipe.app.browser;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileTransferMode;
import io.xpipe.app.browser.file.BrowserFileTransferOperation;
import io.xpipe.app.browser.file.LocalFileSystem;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.Value;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Value
public class BrowserTransferModel {

    private static final Path TEMP = ShellTemp.getLocalTempDataDirectory("download");

    BrowserSessionModel browserSessionModel;
    ObservableList<Item> items = FXCollections.observableArrayList();
    ObservableBooleanValue empty = Bindings.createBooleanBinding(() -> items.isEmpty(), items);

    public BrowserTransferModel(BrowserSessionModel browserSessionModel) {
        this.browserSessionModel = browserSessionModel;
        var thread = ThreadHelper.createPlatformThread("file downloader", true, () -> {
            while (true) {
                Optional<Item> toDownload;
                synchronized (items) {
                    toDownload = items.stream()
                            .filter(item -> !item.downloadFinished().get())
                            .findFirst();
                }
                if (toDownload.isPresent()) {
                    downloadSingle(toDownload.get());
                }
                ThreadHelper.sleep(20);
            }
        });
        thread.start();
    }

    public List<Item> getCurrentItems() {
        synchronized (items) {
            return new ArrayList<>(items);
        }
    }

    private void cleanItem(Item item) {
        if (!Files.isDirectory(TEMP)) {
            return;
        }

        if (!Files.exists(item.getLocalFile())) {
            return;
        }

        try {
            FileUtils.forceDelete(item.getLocalFile().toFile());
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    public void clear(boolean delete) {
        List<Item> toClear;
        synchronized (items) {
            toClear =
                    items.stream().filter(item -> item.downloadFinished().get()).toList();
            if (toClear.isEmpty()) {
                return;
            }
            items.removeAll(toClear);
        }
        if (delete) {
            for (Item item : toClear) {
                cleanItem(item);
            }
        }
    }

    public void drop(OpenFileSystemModel model, List<BrowserEntry> entries) {
        synchronized (items) {
            entries.forEach(entry -> {
                var name = entry.getFileName();
                if (items.stream().anyMatch(item -> item.getName().equals(name))) {
                    return;
                }

                Path file = TEMP.resolve(name);
                var item = new Item(model, name, entry, file);
                items.add(item);
            });
        }
    }

    public void downloadSingle(Item item) {
        try {
            FileUtils.forceMkdir(TEMP.toFile());
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).handle();
            return;
        }

        if (item.downloadFinished().get()) {
            return;
        }

        if (item.getOpenFileSystemModel() != null
                && item.getOpenFileSystemModel().isClosed()) {
            return;
        }

        try {
            var op = new BrowserFileTransferOperation(
                    LocalFileSystem.getLocalFileEntry(TEMP),
                    List.of(item.getBrowserEntry().getRawFileEntry()),
                    BrowserFileTransferMode.COPY,
                    false,
                    progress -> {
                        synchronized (item.getProgress()) {
                            item.getProgress().setValue(progress);
                        }
                        item.getOpenFileSystemModel().getProgress().setValue(progress);
                    });
            op.execute();
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).handle();
            synchronized (items) {
                items.remove(item);
            }
        }
    }

    public void transferToDownloads() throws Exception {
        List<Item> toMove;
        synchronized (items) {
            toMove =
                    items.stream().filter(item -> item.downloadFinished().get()).toList();
            if (toMove.isEmpty()) {
                return;
            }
            items.removeAll(toMove);
        }

        var files = toMove.stream().map(item -> item.getLocalFile()).toList();
        var downloads = DesktopHelper.getDownloadsDirectory();
        for (Path file : files) {
            Files.move(file, downloads.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        DesktopHelper.browseFileInDirectory(downloads.resolve(files.getFirst().getFileName()));
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
