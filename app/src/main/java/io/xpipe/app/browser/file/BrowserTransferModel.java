package io.xpipe.app.browser.file;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.app.util.ThreadHelper;

import io.xpipe.core.process.OsType;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Value
public class BrowserTransferModel {

    private static final Path TEMP = ShellTemp.getLocalTempDataDirectory("download");

    BrowserFullSessionModel browserSessionModel;
    ObservableList<Item> items = FXCollections.observableArrayList();
    ObservableBooleanValue empty = Bindings.createBooleanBinding(() -> items.isEmpty(), items);
    BooleanProperty transferring = new SimpleBooleanProperty();

    public BrowserTransferModel(BrowserFullSessionModel browserSessionModel) {
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
                } else {
                    ThreadHelper.sleep(20);
                }
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

    public void drop(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        synchronized (items) {
            entries.forEach(entry -> {
                var name = entry.getFileName();
                if (items.stream().anyMatch(item -> item.getName().equals(name))) {
                    return;
                }

                var fixedFile = entry.getRawFileEntry().getPath().fileSystemCompatible(OsType.getLocal());
                Path file = TEMP.resolve(fixedFile.getFileName());
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

        var itemModel = item.getOpenFileSystemModel();
        if (itemModel == null || itemModel.isClosed()) {
            return;
        }

        try {
            transferring.setValue(true);
            var op = new BrowserFileTransferOperation(
                    BrowserLocalFileSystem.getLocalFileEntry(TEMP),
                    List.of(item.getBrowserEntry().getRawFileEntry()),
                    BrowserFileTransferMode.COPY,
                    false,
                    progress -> {
                        // Don't update item progress to keep it as finished
                        if (progress == null) {
                            itemModel.getProgress().setValue(null);
                            return;
                        }

                        synchronized (item.getProgress()) {
                            item.getProgress().setValue(progress);
                        }
                        itemModel.getProgress().setValue(progress);
                    },
                    itemModel.getTransferCancelled());
            op.execute();
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).handle();
            synchronized (items) {
                items.remove(item);
            }
        } finally {
            transferring.setValue(false);
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
        var downloads = getDownloadsTargetDirectory();
        Files.createDirectories(downloads);
        for (Path file : files) {
            if (!Files.exists(file)) {
                continue;
            }

            var target = downloads.resolve(file.getFileName());
            // Prevent DirectoryNotEmptyException
            if (Files.exists(target) && Files.isDirectory(target)) {
                FileUtils.deleteDirectory(target.toFile());
            }
            if (Files.isDirectory(file)) {
                FileUtils.moveDirectory(file.toFile(), target.toFile());
            } else {
                Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        DesktopHelper.browseFileInDirectory(downloads.resolve(files.getFirst().getFileName()));
    }

    private Path getDownloadsTargetDirectory() throws Exception {
        var def = DesktopHelper.getDownloadsDirectory();
        var custom = AppPrefs.get().downloadsDirectory().getValue();
        if (custom == null || custom.isBlank()) {
            return def;
        }

        try {
            var path = Path.of(custom);
            if (Files.isDirectory(path)) {
                return path;
            }
        } catch (InvalidPathException ignored) {
        }
        return def;
    }

    @Value
    public static class Item {
        BrowserFileSystemTabModel openFileSystemModel;
        String name;
        BrowserEntry browserEntry;
        Path localFile;
        Property<BrowserTransferProgress> progress;

        public Item(
                BrowserFileSystemTabModel openFileSystemModel, String name, BrowserEntry browserEntry, Path localFile) {
            this.openFileSystemModel = openFileSystemModel;
            this.name = name;
            this.browserEntry = browserEntry;
            this.localFile = localFile;
            this.progress = new SimpleObjectProperty<>();
        }

        public ObservableBooleanValue downloadFinished() {
            synchronized (progress) {
                return Bindings.createBooleanBinding(
                        () -> {
                            return progress.getValue() != null
                                    && progress.getValue().done();
                        },
                        progress);
            }
        }
    }
}
