package io.xpipe.app.util;

import io.xpipe.app.core.*;
import io.xpipe.app.fxcomps.impl.SecretFieldComp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.util.InPlaceSecretValue;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UnlockAlert {

    public static void showIfNeeded() throws Exception {
        if (AppPrefs.get().getLockCrypt().getValue() == null
                || AppPrefs.get().getLockCrypt().getValue().isEmpty()) {
            return;
        }

        if (AppPrefs.get().getLockPassword().getValue() != null) {
            return;
        }

        PlatformState.initPlatformOrThrow();
        AppI18n.init();
        AppStyle.init();
        AppTheme.init();

        while (true) {
            var pw = new SimpleObjectProperty<InPlaceSecretValue>();
            var canceled = new SimpleBooleanProperty();
            AppWindowHelper.showBlockingAlert(alert -> {
                        alert.setTitle(AppI18n.get("unlockAlertTitle"));
                        alert.setHeaderText(AppI18n.get("unlockAlertHeader"));
                        alert.setAlertType(Alert.AlertType.CONFIRMATION);

                        var text = new SecretFieldComp(pw).createRegion();
                        text.setStyle("-fx-border-width: 1px");

                        var content = new VBox(text);
                        content.setSpacing(5);
                        alert.getDialogPane().setContent(content);

                        var stage = (Stage) alert.getDialogPane().getScene().getWindow();
                        stage.setAlwaysOnTop(true);

                        alert.setOnShown(event -> {
                            stage.requestFocus();
                            // Wait 1 pulse before focus so that the scene can be assigned to text
                            Platform.runLater(text::requestFocus);
                            event.consume();
                        });
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
