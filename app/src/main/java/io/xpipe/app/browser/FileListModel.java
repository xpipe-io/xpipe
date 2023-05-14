/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.FileOpener;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.store.FileSystem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Getter
public final class FileListModel {

    static final Comparator<FileBrowserEntry> FILE_TYPE_COMPARATOR =
            Comparator.comparing(path -> !path.getRawFileEntry().isDirectory());
    static final Predicate<FileBrowserEntry> PREDICATE_ANY = path -> true;
    static final Predicate<FileBrowserEntry> PREDICATE_NOT_HIDDEN = path -> true;

    private final OpenFileSystemModel fileSystemModel;
    private final Property<Comparator<FileBrowserEntry>> comparatorProperty =
            new SimpleObjectProperty<>(FILE_TYPE_COMPARATOR);
    private final Property<List<FileBrowserEntry>> all = new SimpleObjectProperty<>(new ArrayList<>());
    private final Property<List<FileBrowserEntry>> shown = new SimpleObjectProperty<>(new ArrayList<>());
    private final ObjectProperty<Predicate<FileBrowserEntry>> predicateProperty =
            new SimpleObjectProperty<>(path -> true);
    private final ObservableList<FileBrowserEntry> selected = FXCollections.observableArrayList();
    private final ObservableList<FileSystem.FileEntry> selectedRaw =
            BindingsHelper.mappedContentBinding(selected, entry -> entry.getRawFileEntry());

    private final Property<FileBrowserEntry> draggedOverDirectory = new SimpleObjectProperty<FileBrowserEntry>();
    private final Property<Boolean> draggedOverEmpty = new SimpleBooleanProperty();
    private final Property<FileBrowserEntry> editing = new SimpleObjectProperty<>();

    public FileListModel(OpenFileSystemModel fileSystemModel) {
        this.fileSystemModel = fileSystemModel;

        fileSystemModel.getFilter().addListener((observable, oldValue, newValue) -> {
            refreshShown();
        });
    }

    public FileBrowserModel.Mode getMode() {
        return fileSystemModel.getBrowserModel().getMode();
    }

    public void setAll(Stream<FileSystem.FileEntry> newFiles) {
        try (var s = newFiles) {
            var parent = fileSystemModel.getCurrentParentDirectory();
            var l = Stream.concat(
                            parent != null ? Stream.of(new FileBrowserEntry(parent, this, true)) : Stream.of(),
                            s.filter(entry -> entry != null)
                                    .limit(5000)
                                    .map(entry -> new FileBrowserEntry(entry, this, false)))
                    .toList();
            all.setValue(l);
            refreshShown();
        }
    }

    public void setComparator(Comparator<FileBrowserEntry> comparator) {
        comparatorProperty.setValue(comparator);
        refreshShown();
    }

    private void refreshShown() {
        List<FileBrowserEntry> filtered = fileSystemModel.getFilter().getValue() != null
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

        Comparator<FileBrowserEntry> tableComparator = comparatorProperty.getValue();
        var comparator =
                tableComparator != null ? FILE_TYPE_COMPARATOR.thenComparing(tableComparator) : FILE_TYPE_COMPARATOR;
        var listCopy = new ArrayList<>(filtered);
        listCopy.sort(comparator);
        shown.setValue(listCopy);
    }

    public boolean rename(String filename, String newName) {
        var fullPath = FileNames.join(fileSystemModel.getCurrentPath().get(), filename);
        var newFullPath = FileNames.join(fileSystemModel.getCurrentPath().get(), newName);
        try {
            fileSystemModel.getFileSystem().move(fullPath, newFullPath);
            fileSystemModel.refresh();
            return true;
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            return false;
        }
    }

    public void onDoubleClick(FileBrowserEntry entry) {
        if (!entry.getRawFileEntry().isDirectory() && getMode().equals(FileBrowserModel.Mode.SINGLE_FILE_CHOOSER)) {
            getFileSystemModel().getBrowserModel().finishChooser();
            return;
        }

        if (entry.getRawFileEntry().isDirectory()) {
            var dir = fileSystemModel.cd(entry.getRawFileEntry().getPath());
            if (dir.isPresent()) {
                fileSystemModel.cd(dir.get());
            }
        } else {
            FileOpener.openInTextEditor(entry.getRawFileEntry());
        }
    }

    public ObjectProperty<Predicate<FileBrowserEntry>> predicateProperty() {
        return predicateProperty;
    }
}
