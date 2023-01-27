package io.xpipe.app.prefs;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.extension.I18n;
import javafx.scene.control.Alert;

public class ClearCacheAlert {

    public static void show() {
        AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(I18n.get("clearCachesAlertTitle"));
                    alert.setHeaderText(I18n.get("clearCachesAlertTitleHeader"));
                    alert.getDialogPane()
                            .setContent(AppWindowHelper.alertContentText(I18n.get("clearCachesAlertTitleContent")));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                })
                .filter(b -> b.getButtonData().isDefaultButton())
                .ifPresent(t -> {
                    AppCache.clear();
                });
    }
}
