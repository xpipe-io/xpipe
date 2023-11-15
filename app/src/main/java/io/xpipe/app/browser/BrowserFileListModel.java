package io.xpipe.app.browser;

import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileSystem;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Getter
public final class BrowserFileListModel {

    static final Comparator<BrowserEntry> FILE_TYPE_COMPARATOR =
            Comparator.comparing(path -> path.getRawFileEntry().resolved().getKind() != FileKind.DIRECTORY);

    private final OpenFileSystemModel fileSystemModel;
    private final Property<Comparator<BrowserEntry>> comparatorProperty =
            new SimpleObjectProperty<>(FILE_TYPE_COMPARATOR);
    private final Property<List<BrowserEntry>> all = new SimpleObjectProperty<>(new ArrayList<>());
    private final Property<List<BrowserEntry>> shown = new SimpleObjectProperty<>(new ArrayList<>());
    private final ObservableList<BrowserEntry> previousSelection = FXCollections.observableArrayList();
    private final ObservableList<BrowserEntry> selection = FXCollections.observableArrayList();
    private final ObservableList<FileSystem.FileEntry> selectedRaw =
            BindingsHelper.mappedContentBinding(selection, entry -> entry.getRawFileEntry());

    private final Property<BrowserEntry> draggedOverDirectory = new SimpleObjectProperty<>();
    private final Property<Boolean> draggedOverEmpty = new SimpleBooleanProperty();
    private final Property<BrowserEntry> editing = new SimpleObjectProperty<>();

    public BrowserFileListModel(OpenFileSystemModel fileSystemModel) {
        this.fileSystemModel = fileSystemModel;

        fileSystemModel.getFilter().addListener((observable, oldValue, newValue) -> {
            refreshShown();
        });

        selection.addListener((ListChangeListener<? super BrowserEntry>) c -> {
            previousSelection.setAll(c.getList());
        });
    }

    public BrowserModel.Mode getMode() {
        return fileSystemModel.getBrowserModel().getMode();
    }

    public void setAll(Stream<FileSystem.FileEntry> newFiles) {
        try (var s = newFiles) {
            var parent = fileSystemModel.getCurrentParentDirectory();
            var l = Stream.concat(
                            parent != null ? Stream.of(new BrowserEntry(parent, this, true)) : Stream.of(),
                            s.filter(entry -> entry != null)
                                    .limit(5000)
                                    .map(entry -> new BrowserEntry(entry, this, false)))
                    .toList();
            all.setValue(l);
            refreshShown();
        }
    }

    public void setComparator(Comparator<BrowserEntry> comparator) {
        comparatorProperty.setValue(comparator);
        refreshShown();
    }

    private void refreshShown() {
        List<BrowserEntry> filtered = fileSystemModel.getFilter().getValue() != null
                ? all.getValue().stream()
                        .filter(entry -> {
                            var name = FileNames.getFileName(
                                            entry.getRawFileEntry().getPath())
                                    .toLowerCase(Locale.ROOT);
                            var filterString =
                                    fileSystemModel.getFilter().getValue().toLowerCase(Locale.ROOT);
                            return name.contains(filterString);
                        })
                        .toList()
                : all.getValue();

        var listCopy = new ArrayList<>(filtered);
        sort(listCopy);
        shown.setValue(listCopy);
    }

    private void sort(List<BrowserEntry> l) {
        var syntheticFirst = Comparator.<BrowserEntry, Boolean>comparing(path -> !path.isSynthetic());
        var dirsFirst = Comparator.<BrowserEntry, Boolean>comparing(
                path -> path.getRawFileEntry().resolved().getKind() != FileKind.DIRECTORY);
        var comp = comparatorProperty.getValue();

        Comparator<? super BrowserEntry> us =
                comp != null ? syntheticFirst.thenComparing(dirsFirst).thenComparing(comp) : syntheticFirst.thenComparing(dirsFirst);
        l.sort(us);
    }

    public boolean rename(String filename, String newName) {
        var fullPath = FileNames.join(fileSystemModel.getCurrentPath().get(), filename);
        var newFullPath = FileNames.join(fileSystemModel.getCurrentPath().get(), newName);

        boolean exists;
        try {
            exists = fileSystemModel.getFileSystem().fileExists(newFullPath) || fileSystemModel.getFileSystem().directoryExists(newFullPath);
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            return false;
        }

        if (exists) {
            ErrorEvent.fromMessage("Target " + newFullPath + " does already exist").expected().handle();
            fileSystemModel.refresh();
            return false;
        }

        try {
            fileSystemModel.getFileSystem().move(fullPath, newFullPath);
            fileSystemModel.refresh();
            return true;
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            return false;
        }
    }

    public void onDoubleClick(BrowserEntry entry) {
        if (entry.getRawFileEntry().resolved().getKind() != FileKind.DIRECTORY
                && getMode().equals(BrowserModel.Mode.SINGLE_FILE_CHOOSER)) {
            getFileSystemModel().getBrowserModel().finishChooser();
            return;
        }

        if (entry.getRawFileEntry().resolved().getKind() == FileKind.DIRECTORY) {
            fileSystemModel.cdAsync(entry.getRawFileEntry().resolved().getPath());
        }
    }
}
