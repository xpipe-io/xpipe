package io.xpipe.app.browser;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileSystem;
import javafx.scene.control.Alert;

import java.util.List;
import java.util.stream.Collectors;

public class BrowserAlerts {

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
}
