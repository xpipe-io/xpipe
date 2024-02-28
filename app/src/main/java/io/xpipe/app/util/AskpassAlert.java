package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.impl.SecretFieldComp;
import io.xpipe.core.util.InPlaceSecretValue;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class AskpassAlert {

    public static SecretQueryResult queryRaw(String prompt, InPlaceSecretValue secretValue) {
        var prop = new SimpleObjectProperty<>(secretValue);
        var r = AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("askpassAlertTitle"));
                    alert.setHeaderText(prompt);
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);

                    var text = new SecretFieldComp(prop).createStructure().get();
                    alert.getDialogPane().setContent(new StackPane(text));
                    var stage = (Stage) alert.getDialogPane().getScene().getWindow();
                    stage.setAlwaysOnTop(true);

                    var anim = new AnimationTimer() {

                        private long lastRun = 0;

                        @Override
                        public void handle(long now) {
                            if (lastRun == 0) {
                                lastRun = now;
                                return;
                            }

                            long elapsed = (now - lastRun) / 1_000_000;
                            if (elapsed < 1000) {
                                return;
                            }

                            stage.requestFocus();
                            lastRun = now;
                        }
                    };

                    alert.setOnShown(event -> {
                        stage.requestFocus();
                        anim.start();
                        // Wait 1 pulse before focus so that the scene can be assigned to text
                        Platform.runLater(() -> {
                            text.requestFocus();
                            text.end();
                        });
                        event.consume();
                    });

                    alert.setOnHiding(event -> {
                        anim.stop();
                    });
                })
                .filter(b -> b.getButtonData().isDefaultButton())
                .map(t -> {
                    return prop.getValue() != null ? prop.getValue() : InPlaceSecretValue.of("");
                })
                .orElse(null);
        return new SecretQueryResult(r, r == null);
    }
}
