package io.xpipe.app.browser.file;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FilePath;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class BrowserAlerts {

    public static FileConflictChoice showFileConflictAlert(FilePath file, boolean multiple) {
        var choice = new SimpleObjectProperty<FileConflictChoice>();
        var key = multiple ? "fileConflictAlertContentMultiple" : "fileConflictAlertContent";
        var w = multiple ? 700 : 400;
        var modal = ModalOverlay.of("fileConflictAlertTitle", AppDialog.dialogText(AppI18n.observable(key, file)).prefWidth(w));
        modal.addButton(new ModalButton("cancel", () -> choice.set(FileConflictChoice.CANCEL), true, false));
        if (multiple) {
            modal.addButton(new ModalButton("skip", () -> choice.set(FileConflictChoice.SKIP), true, false));
            modal.addButton(new ModalButton("skipAll", () -> choice.set(FileConflictChoice.SKIP_ALL), true, false));
        }
        modal.addButton(new ModalButton("replace", () -> choice.set(FileConflictChoice.REPLACE), true, false));
        if (multiple) {
            modal.addButton(new ModalButton("replaceAll", () -> choice.set(FileConflictChoice.REPLACE_ALL), true, false));
        }
        modal.addButton(new ModalButton("rename", () -> choice.set(FileConflictChoice.RENAME), true, false));
        if (multiple) {
            modal.addButton(new ModalButton("renameAll", () -> choice.set(FileConflictChoice.RENAME_ALL), true, false));
        }
        modal.showAndWait();
        return choice.get() != null ? choice.get() : FileConflictChoice.CANCEL;
    }

    public static boolean showMoveAlert(List<FileEntry> source, FileEntry target) {
        if (source.stream().noneMatch(entry -> entry.getKind() == FileKind.DIRECTORY)) {
            return true;
        }

        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("moveAlertTitle"));
                    alert.setHeaderText(AppI18n.get("moveAlertHeader", source.size(), target.getPath()));
                    alert.getDialogPane()
                            .setContent(AppWindowHelper.alertContentText(getSelectedElementsString(source)));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                })
                .map(b -> b.getButtonData().isDefaultButton())
                .orElse(false);
    }

    public static boolean showDeleteAlert(BrowserFileSystemTabModel model, List<FileEntry> source) {
        var config =
                DataStorage.get().getEffectiveCategoryConfig(model.getEntry().get());
        if (!Boolean.TRUE.equals(config.getConfirmAllModifications())
                && source.stream().noneMatch(entry -> entry.getKind() == FileKind.DIRECTORY)) {
            return true;
        }

        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("deleteAlertTitle"));
                    alert.setHeaderText(AppI18n.get("deleteAlertHeader", source.size()));
                    alert.getDialogPane()
                            .setContent(AppWindowHelper.alertContentText(getSelectedElementsString(source)));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                })
                .map(b -> b.getButtonData().isDefaultButton())
                .orElse(false);
    }

    private static String getSelectedElementsString(List<FileEntry> source) {
        var namesHeader = AppI18n.get("selectedElements");
        var names = namesHeader + "\n"
                + source.stream()
                        .limit(10)
                        .map(entry -> "- " + entry.getPath().getFileName())
                        .collect(Collectors.joining("\n"));
        if (source.size() > 10) {
            names += "\n+ " + (source.size() - 10) + " ...";
        }
        return names;
    }

    public enum FileConflictChoice {
        CANCEL,
        SKIP,
        SKIP_ALL,
        REPLACE,
        REPLACE_ALL,
        RENAME,
        RENAME_ALL
    }
}
