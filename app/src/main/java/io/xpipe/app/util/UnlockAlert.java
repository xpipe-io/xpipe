package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.impl.SecretFieldComp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.util.SecretValue;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;

public class UnlockAlert {

    public static void showIfNeeded() {
        if (AppPrefs.get().getLockCrypt().getValue() == null
                || AppPrefs.get().getLockCrypt().getValue().isEmpty()) {
            return;
        }

        if (AppPrefs.get().getLockPassword().getValue() != null) {
            return;
        }

        while (true) {
            var pw = new SimpleObjectProperty<SecretValue>();
            var canceled = new SimpleBooleanProperty();
            AppWindowHelper.showBlockingAlert(alert -> {
                        alert.setTitle(AppI18n.get("unlockAlertTitle"));
                        alert.setHeaderText(AppI18n.get("unlockAlertHeader"));
                        alert.setAlertType(Alert.AlertType.CONFIRMATION);

                        var p1 = new SecretFieldComp(pw) {
                            @Override
                            protected SecretValue encrypt(char[] c) {
                                return SecretHelper.encryptInPlace(c);
                            }
                        }.createRegion();
                        p1.setStyle("-fx-border-width: 1px");

                        var content = new VBox(p1);
                        content.setSpacing(5);
                        alert.getDialogPane().setContent(content);
                    })
                    .filter(b -> b.getButtonData().isDefaultButton())
                    .ifPresentOrElse(t -> {}, () -> canceled.set(true));

            if (canceled.get()) {
                ErrorEvent.fromMessage("Unlock cancelled").expected().term().omit().handle();
                return;
            }

            if (AppPrefs.get().unlock(pw.get())) {
                return;
            }
        }
    }
}
