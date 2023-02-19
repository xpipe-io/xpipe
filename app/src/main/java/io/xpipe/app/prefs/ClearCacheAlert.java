package io.xpipe.app.prefs;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import javafx.scene.control.Alert;

public class ClearCacheAlert {

    public static void show() {
        AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("clearCachesAlertTitle"));
                    alert.setHeaderText(AppI18n.get("clearCachesAlertTitleHeader"));
                    alert.getDialogPane()
                            .setContent(AppWindowHelper.alertContentText(AppI18n.get("clearCachesAlertTitleContent")));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                })
                .filter(b -> b.getButtonData().isDefaultButton())
                .ifPresent(t -> {
                    AppCache.clear();
                });
    }
}
