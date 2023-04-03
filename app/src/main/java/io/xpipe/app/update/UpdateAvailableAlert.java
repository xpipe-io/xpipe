package io.xpipe.app.update;

import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

public class UpdateAvailableAlert {

    public static void showIfNeeded() {
        if (AppUpdater.get().getDownloadedUpdate().getValue() == null) {
            return;
        }

        var u = AppUpdater.get().getDownloadedUpdate().getValue();
        var update = AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("updateReadyAlertTitle"));
                    alert.setAlertType(Alert.AlertType.NONE);

                    if (u.getBody() != null && !u.getBody().isBlank()) {
                        var markdown = new MarkdownComp(u.getBody(), s -> {
                                    var header = "<h1>" + AppI18n.get("whatsNew", u.getVersion()) + "</h1>";
                                    return header + s;
                                })
                                .createRegion();
                        alert.getDialogPane().setContent(markdown);
                    } else {
                        alert.getDialogPane()
                                .setContent(AppWindowHelper.alertContentText(AppI18n.get("updateReadyAlertContent")));
                    }

                    alert.getButtonTypes().clear();
                    alert.getButtonTypes().add(new ButtonType(AppI18n.get("update"), ButtonBar.ButtonData.OK_DONE));
                    alert.getButtonTypes().add(new ButtonType(AppI18n.get("ignore"), ButtonBar.ButtonData.NO));
                })
                .map(buttonType -> buttonType.getButtonData().isDefaultButton())
                .orElse(false);
        if (update) {
            AppUpdater.get().executeUpdateAndClose();
        }
    }
}
