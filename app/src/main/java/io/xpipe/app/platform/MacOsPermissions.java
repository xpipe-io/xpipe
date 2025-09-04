package io.xpipe.app.platform;

import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Alert;

import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public class MacOsPermissions {

    public static boolean waitForAccessibilityPermissions() throws Exception {
        AtomicReference<Alert> alert = new AtomicReference<>();
        var state = new SimpleBooleanProperty(true);
        try (var pc = LocalShell.getShell().start()) {
            while (state.get()) {
                // We can't wait in the platform thread, so just return instantly
                if (Platform.isFxApplicationThread()) {
                    pc.osascriptCommand(
                                    """
                                        tell application "System Events" to keystroke "t"
                                        """)
                            .execute();
                    return true;
                }

                var success = pc.osascriptCommand(
                                """
                                                  tell application "System Events" to keystroke "t"
                                                  """)
                        .executeAndCheck();

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

                        //                        AppWindowHelper.showAlert(
                        //                                a -> {
                        //                                    a.setAlertType(Alert.AlertType.INFORMATION);
                        //                                    a.setTitle(AppI18n.get("permissionsAlertTitle"));
                        //                                    a.setHeaderText(AppI18n.get("permissionsAlertHeader"));
                        //                                    a.getDialogPane()
                        //                                            .setContent(AppWindowHelper.alertContentText(
                        //                                                    AppI18n.get("permissionsAlertContent")));
                        //                                    a.getButtonTypes().clear();
                        //                                    a.getButtonTypes().add(ButtonType.CANCEL);
                        //                                    alert.set(a);
                        //                                },
                        //                                buttonType -> {
                        //                                    alert.get().close();
                        //                                    state.set(false);
                        //                                });
                    });
                    ThreadHelper.sleep(1000);
                }
            }
        }

        return false;
    }
}
