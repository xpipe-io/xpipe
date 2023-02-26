/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.FileOpener;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.store.FileSystem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

@Getter
final class FileListModel {

    static final Comparator<FileSystem.FileEntry> FILE_TYPE_COMPARATOR =
            Comparator.comparing(path -> !path.isDirectory());
    static final Predicate<FileSystem.FileEntry> PREDICATE_ANY = path -> true;
    static final Predicate<FileSystem.FileEntry> PREDICATE_NOT_HIDDEN = path -> true;

    private final OpenFileSystemModel model;
    private final Property<Comparator<FileSystem.FileEntry>> comparatorProperty =
            new SimpleObjectProperty<>(FILE_TYPE_COMPARATOR);
    private final Property<List<FileSystem.FileEntry>> all = new SimpleObjectProperty<>(List.of());
    private final Property<List<FileSystem.FileEntry>> shown = new SimpleObjectProperty<>(List.of());
    private final ObjectProperty<Predicate<FileSystem.FileEntry>> predicateProperty =
            new SimpleObjectProperty<>(path -> true);

    public FileListModel(OpenFileSystemModel model) {
        this.model = model;

        model.getFilter().addListener((observable, oldValue, newValue) -> {
            refreshShown();
        });
    }

    public FileBrowserModel.Mode getMode() {
        return model.getBrowserModel().getMode();
    }

    public void setAll(List<FileSystem.FileEntry> newFiles) {
        all.setValue(newFiles);
        refreshShown();
    }

    public void setComparator(Comparator<FileSystem.FileEntry> comparator) {
        comparatorProperty.setValue(comparator);
        refreshShown();
    }

    private void refreshShown() {
        List<FileSystem.FileEntry> filtered = model.getFilter().getValue() != null ? all.getValue().stream().filter(entry -> {
            var name = FileNames.getFileName(entry.getPath()).toLowerCase(Locale.ROOT);
            var filterString = model.getFilter().getValue().toLowerCase(Locale.ROOT);
            return name.contains(filterString);
        }).toList() : all.getValue();

        Comparator<FileSystem.FileEntry> tableComparator = comparatorProperty.getValue();
        var comparator =  tableComparator != null
                ? FILE_TYPE_COMPARATOR.thenComparing(tableComparator)
                : FILE_TYPE_COMPARATOR;
        var listCopy = new ArrayList<>(filtered);
        listCopy.sort(comparator);
        shown.setValue(listCopy);
    }

    public boolean rename(String filename, String newName) {
        var fullPath = FileNames.join(model.getCurrentPath().get(), filename);
        var newFullPath = FileNames.join(model.getCurrentPath().get(), newName);
        try {
            model.getFileSystem().move(fullPath, newFullPath);
            model.refresh();
            return true;
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            return false;
        }
    }

    public void onDoubleClick(FileSystem.FileEntry entry) {
        if (!entry.isDirectory() && getMode().equals(FileBrowserModel.Mode.SINGLE_FILE_CHOOSER)) {
            getModel().getBrowserModel().finishChooser();
            return;
        }

        if (entry.isDirectory()) {
            model.navigate(entry.getPath(), true);
        } else {
            FileOpener.openInTextEditor(entry);
        }
    }

    public ObjectProperty<Predicate<FileSystem.FileEntry>> predicateProperty() {
        return predicateProperty;
    }
}
