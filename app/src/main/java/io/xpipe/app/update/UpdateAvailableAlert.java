package io.xpipe.app.update;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import javafx.scene.control.Alert;

public class UpdateAvailableAlert {

    public static void showIfNeeded() {
        if (AppUpdater.get().getDownloadedUpdate().getValue() == null) {
            return;
        }

        if (AppUpdater.get().getDownloadedUpdate().getValue() != null && !AppUpdater.get().isDownloadedUpdateStillLatest()) {
            AppUpdater.get().getDownloadedUpdate().setValue(null);
            return;
        }

        var update = AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("updateReadyAlertTitle"));
                    alert.setHeaderText(AppI18n.get("updateReadyAlertHeader", AppUpdater.get().getDownloadedUpdate().getValue().getVersion()));
                    alert.getDialogPane().setContent(AppWindowHelper.alertContentText(AppI18n.get("updateReadyAlertContent")));
                    alert.setAlertType(Alert.AlertType.INFORMATION);
                })
                .map(buttonType -> buttonType.getButtonData().isDefaultButton())
                .orElse(false);
        if (update) {
            AppUpdater.get().executeUpdateAndClose();
        }
    }
}
