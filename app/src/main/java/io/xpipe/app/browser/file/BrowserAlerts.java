package io.xpipe.app.browser.file;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FilePath;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class BrowserAlerts {

    public static FileConflictChoice showFileConflictAlert(FilePath file, boolean multiple) {
        var map = new LinkedHashMap<ButtonType, FileConflictChoice>();
        map.put(new ButtonType(AppI18n.get("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE), FileConflictChoice.CANCEL);
        if (multiple) {
            map.put(new ButtonType(AppI18n.get("skip"), ButtonBar.ButtonData.OTHER), FileConflictChoice.SKIP);
            map.put(new ButtonType(AppI18n.get("skipAll"), ButtonBar.ButtonData.OTHER), FileConflictChoice.SKIP_ALL);
        }
        map.put(new ButtonType(AppI18n.get("replace"), ButtonBar.ButtonData.OTHER), FileConflictChoice.REPLACE);
        if (multiple) {
            map.put(
                    new ButtonType(AppI18n.get("replaceAll"), ButtonBar.ButtonData.OTHER),
                    FileConflictChoice.REPLACE_ALL);
        }
        map.put(new ButtonType(AppI18n.get("rename"), ButtonBar.ButtonData.OTHER), FileConflictChoice.RENAME);
        if (multiple) {
            map.put(
                    new ButtonType(AppI18n.get("renameAll"), ButtonBar.ButtonData.OTHER),
                    FileConflictChoice.RENAME_ALL);
        }
        var w = multiple ? 700 : 400;
        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("fileConflictAlertTitle"));
                    alert.setHeaderText(AppI18n.get("fileConflictAlertHeader"));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                    alert.getButtonTypes().clear();
                    alert.getDialogPane()
                            .setContent(AppWindowHelper.alertContentText(
                                    AppI18n.get(
                                            multiple ? "fileConflictAlertContentMultiple" : "fileConflictAlertContent",
                                            file),
                                    w - 50));
                    alert.getDialogPane().setMinWidth(w);
                    alert.getDialogPane().setPrefWidth(w);
                    alert.getDialogPane().setMaxWidth(w);
                    map.sequencedKeySet()
                            .forEach(buttonType -> alert.getButtonTypes().add(buttonType));
                })
                .map(map::get)
                .orElse(FileConflictChoice.CANCEL);
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
        var config = DataStorage.get().getEffectiveCategoryConfig(model.getEntry().get());
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
