package io.xpipe.app.browser.file;

import io.xpipe.app.browser.action.impl.MoveFileActionProvider;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.FileKind;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.Getter;

import java.util.*;
import java.util.stream.Stream;

@Getter
public final class BrowserFileListModel {

    static final Comparator<BrowserEntry> FILE_TYPE_COMPARATOR =
            Comparator.comparing(path -> path.getRawFileEntry().resolved().getKind() != FileKind.DIRECTORY);

    private final BrowserFileSystemTabModel.SelectionMode selectionMode;

    private final BrowserFileSystemTabModel fileSystemModel;
    private final Property<Comparator<BrowserEntry>> comparatorProperty =
            new SimpleObjectProperty<>(FILE_TYPE_COMPARATOR);
    private final Property<List<BrowserEntry>> all = new SimpleObjectProperty<>(new ArrayList<>());
    private final Property<List<BrowserEntry>> shown = new SimpleObjectProperty<>(new ArrayList<>());
    private final ObservableList<BrowserEntry> selection = FXCollections.observableArrayList();

    private final Property<BrowserEntry> draggedOverDirectory = new SimpleObjectProperty<>();
    private final Property<Boolean> draggedOverEmpty = new SimpleBooleanProperty();
    private final Property<BrowserEntry> editing = new SimpleObjectProperty<>();

    public BrowserFileListModel(
            BrowserFileSystemTabModel.SelectionMode selectionMode, BrowserFileSystemTabModel fileSystemModel) {
        this.selectionMode = selectionMode;
        this.fileSystemModel = fileSystemModel;

        fileSystemModel.getFilter().addListener((observable, oldValue, newValue) -> {
            refreshShown();
        });
    }

    public void setAll(Stream<FileEntry> newFiles) {
        try (var s = newFiles) {
            var l = s.filter(entry -> entry != null)
                    .map(entry -> new BrowserEntry(entry, this))
                    .toList();
            all.setValue(l);
            refreshShown();
        }
    }

    public void updateEntry(FilePath p, FileEntry n) {
        var found = all.getValue().stream()
                .filter(browserEntry -> browserEntry.getRawFileEntry().getPath().equals(p))
                .findFirst();
        if (found.isEmpty()) {
            return;
        }

        var index = all.getValue().indexOf(found.get());
        var l = new ArrayList<>(all.getValue());
        if (n != null) {
            l.set(index, new BrowserEntry(n, this));
        } else {
            l.remove(index);
        }
        all.setValue(l);
        refreshShown();
    }

    public void setComparator(Comparator<BrowserEntry> comparator) {
        comparatorProperty.setValue(comparator);
        refreshShown();
    }

    void refreshShown() {
        List<BrowserEntry> filtered = fileSystemModel.getFilter().getValue() != null
                ? all.getValue().stream()
                        .filter(entry -> {
                            var name = entry.getRawFileEntry()
                                    .getPath()
                                    .getFileName()
                                    .toLowerCase(Locale.ROOT);
                            var filterString =
                                    fileSystemModel.getFilter().getValue().toLowerCase(Locale.ROOT);
                            return name.contains(filterString);
                        })
                        .toList()
                : all.getValue();

        var listCopy = new ArrayList<>(filtered);
        listCopy.sort(order());
        shown.setValue(listCopy);
    }

    public Comparator<BrowserEntry> order() {
        var dirsFirst = Comparator.<BrowserEntry, Boolean>comparing(
                path -> path.getRawFileEntry().resolved().getKind() != FileKind.DIRECTORY);
        var comp = comparatorProperty.getValue();

        Comparator<BrowserEntry> us = comp != null ? dirsFirst.thenComparing(comp) : dirsFirst;
        return us;
    }

    public BrowserEntry rename(BrowserEntry old, String newName) {
        if (old == null
                || newName == null
                || fileSystemModel == null
                || fileSystemModel.isClosed()
                || fileSystemModel.getCurrentPath().get() == null) {
            return old;
        }

        if (newName.isEmpty() || !newName.strip().equals(newName)) {
            return old;
        }

        var newFullPath = fileSystemModel.getCurrentPath().get().join(newName);

        // This check will fail on case-insensitive file systems when changing the case of the file
        // So skip it in this case
        var skipExistCheck = !fileSystemModel.getFileSystem().isCaseSensitive() && old.getFileName().equalsIgnoreCase(newName);
        if (!skipExistCheck) {
            boolean exists;
            try {
                exists = fileSystemModel.getFileSystem().fileExists(newFullPath)
                        || fileSystemModel.getFileSystem().directoryExists(newFullPath);
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
                return old;
            }

            if (exists) {
                ErrorEventFactory.fromMessage("Target " + newFullPath + " does already exist")
                        .expected()
                        .handle();
                fileSystemModel.refreshSync();
                return old;
            }
        }

        try {
            var builder = MoveFileActionProvider.Action.builder();
            builder.initEntries(fileSystemModel, List.of(old));
            builder.target(newFullPath);
            builder.build().executeSync();

            var b = all.getValue().stream()
                    .filter(browserEntry ->
                            browserEntry.getRawFileEntry().getPath().equals(newFullPath))
                    .findFirst()
                    .orElse(old);
            return b;
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return old;
        }
    }

    public void onDoubleClick(BrowserEntry entry) {
        var r = entry.getRawFileEntry().resolved();
        if (r.getKind() == FileKind.DIRECTORY) {
            fileSystemModel.cdAsync(r.getPath().toString());
        }

        if (AppPrefs.get().editFilesWithDoubleClick().get() && r.getKind() == FileKind.FILE) {
            var selection = new LinkedHashSet<>(getSelection());
            selection.add(entry);
            for (BrowserEntry e : selection) {
                BrowserFileOpener.openInTextEditor(getFileSystemModel(), e.getRawFileEntry());
            }
        }
    }
}
