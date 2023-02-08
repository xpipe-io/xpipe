package io.xpipe.app.update;

import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.extension.I18n;
import javafx.scene.control.Alert;

public class UpdateAvailableAlert {

    public static void showIfNeeded() {
        if (!AppUpdater.get().isDownloadedUpdateStillLatest()) {
            AppUpdater.get().getDownloadedUpdate().setValue(null);
            return;
        }

        var update = AppWindowHelper.showBlockingAlert(
                alert -> {
                    alert.setTitle(I18n.get("updateReadyTitle"));
                    alert.setHeaderText(I18n.get("updateReadyHeader"));
                    alert.setContentText(I18n.get("updateReadyContent"));
                    alert.setAlertType(Alert.AlertType.INFORMATION);
                }).map(buttonType -> buttonType.getButtonData().isDefaultButton()).orElse(false);
        if (update) {
            AppUpdater.get().executeUpdateAndClose();
        }
    }
}
