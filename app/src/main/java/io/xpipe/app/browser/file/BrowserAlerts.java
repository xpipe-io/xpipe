package io.xpipe.app.browser.file;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileSystem;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class BrowserAlerts {

    public static FileConflictChoice showFileConflictAlert(String file, boolean multiple) {
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
        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("fileConflictAlertTitle"));
                    alert.setHeaderText(AppI18n.get("fileConflictAlertHeader"));
                    AppWindowHelper.setContent(
                            alert,
                            AppI18n.get(
                                    multiple ? "fileConflictAlertContentMultiple" : "fileConflictAlertContent", file));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                    alert.getButtonTypes().clear();
                    map.sequencedKeySet()
                            .forEach(buttonType -> alert.getButtonTypes().add(buttonType));
                })
                .map(map::get)
                .orElse(FileConflictChoice.CANCEL);
    }

    public static boolean showMoveAlert(List<FileSystem.FileEntry> source, FileSystem.FileEntry target) {
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

    public static boolean showDeleteAlert(List<FileSystem.FileEntry> source) {
        if (source.stream().noneMatch(entry -> entry.getKind() == FileKind.DIRECTORY)) {
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

    private static String getSelectedElementsString(List<FileSystem.FileEntry> source) {
        var namesHeader = AppI18n.get("selectedElements");
        var names = namesHeader + "\n"
                + source.stream().limit(10).map(entry -> "- " + entry.getPath()).collect(Collectors.joining("\n"));
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
        REPLACE_ALL
    }
}
