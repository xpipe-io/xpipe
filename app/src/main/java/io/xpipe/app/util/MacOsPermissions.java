package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.core.store.ShellStore;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Alert;

import java.util.concurrent.atomic.AtomicReference;

public class MacOsPermissions {

    public static boolean waitForAccessibilityPermissions() throws Exception {
        AtomicReference<Alert> alert = new AtomicReference<>();
        var state = new SimpleBooleanProperty(true);
        try (var pc = ShellStore.local().create().start()) {
            while (state.get()) {
                var success = pc.executeBooleanSimpleCommand(
                        "osascript -e 'tell application \"System Events\" to keystroke \"t\"'");
                if (success) {
                    Platform.runLater(() -> {
                        if (alert.get() != null) {
                            alert.get().close();
                        }
                    });
                    return true;
                } else {
                    Platform.runLater(() -> {
                        if (alert.get() != null) {
                            return;
                        }

                        AppWindowHelper.showAlert(a -> {
                            a.setAlertType(Alert.AlertType.INFORMATION);
                            a.setTitle(AppI18n.get("permissionsAlertTitle"));
                            a.setHeaderText(AppI18n.get("permissionsAlertTitleHeader"));
                            a.getDialogPane().setContent(AppWindowHelper.alertContentText(AppI18n.get("permissionsAlertTitleContent")));
                            a.getButtonTypes().clear();
                            alert.set(a);
                        }, buttonType -> {
                            alert.get().close();
                            if (buttonType.isEmpty() || !buttonType.get().getButtonData().isDefaultButton()) {
                                state.set(false);
                            }
                        });
                    });
                    ThreadHelper.sleep(1000);
                }
            }
        }

        return false;
    }
}
