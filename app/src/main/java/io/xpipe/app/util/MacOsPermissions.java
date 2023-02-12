package io.xpipe.app.util;

import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Alert;

public class MacOsPermissions {

    private static Alert createAlert() {
        var alert = AppWindowHelper.createEmptyAlert();
        alert.setAlertType(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("permissionsAlertTitle"));
        alert.setHeaderText(I18n.get("permissionsAlertTitleHeader"));
        alert.getDialogPane().setContent(AppWindowHelper.alertContentText(I18n.get("permissionsAlertTitleContent")));
        alert.setAlertType(Alert.AlertType.CONFIRMATION);
        return alert;
    }

    public static boolean waitForAccessibilityPermissions() throws Exception {
        var alert = createAlert();
        var state = new SimpleBooleanProperty(true);
        try (var pc = ShellStore.local().create().start()) {
            while (state.get()) {
                var success = pc.executeBooleanSimpleCommand(
                        "osascript -e 'tell application \"System Events\" to keystroke \"t\"'");
                if (success) {
                    Platform.runLater(() -> {
                        alert.close();
                    });
                    return true;
                } else {
                    Platform.runLater(() -> {
                        var result = AppWindowHelper.showBlockingAlert(alert)
                                .map(buttonType -> buttonType.getButtonData().isDefaultButton())
                                .orElse(false);
                        if (!result) {
                            state.set(false);
                        }
                    });
                    ThreadHelper.sleep(1000);
                }
            }
        }

        return false;
    }
}
