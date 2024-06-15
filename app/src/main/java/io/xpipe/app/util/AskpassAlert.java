package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppStyle;
import io.xpipe.app.core.AppTheme;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.impl.SecretFieldComp;
import io.xpipe.core.util.InPlaceSecretValue;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class AskpassAlert {

    public static SecretQueryResult queryRaw(String prompt, InPlaceSecretValue secretValue) {
        if (!PlatformState.initPlatformIfNeeded()) {
            return new SecretQueryResult(null, true);
        }

        AppStyle.init();
        AppTheme.init();

        var prop = new SimpleObjectProperty<>(secretValue);
        var r = AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("askpassAlertTitle"));
                    alert.setHeaderText(prompt);
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);

                    // Link to help page for double prompt
                    if (SecretManager.isSpecialPrompt(prompt)) {
                        var type = new ButtonType("Help", ButtonBar.ButtonData.HELP);
                        alert.getButtonTypes().add(type);
                        var button = alert.getDialogPane().lookupButton(type);
                        button.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                            Hyperlinks.open(Hyperlinks.DOUBLE_PROMPT);
                            event.consume();
                        });
                    }

                    var text = new SecretFieldComp(prop, false).createStructure();
                    alert.getDialogPane().setContent(new StackPane(text.get()));
                    var stage = (Stage) alert.getDialogPane().getScene().getWindow();
                    stage.setAlwaysOnTop(true);

                    var anim = new AnimationTimer() {

                        private long lastRun = 0;
                        private int regainedFocusCount;

                        @Override
                        public void handle(long now) {
                            if (!stage.isShowing()) {
                                return;
                            }

                            if (regainedFocusCount >= 2) {
                                return;
                            }

                            if (lastRun == 0) {
                                lastRun = now;
                                return;
                            }

                            long elapsed = (now - lastRun) / 1_000_000;
                            if (elapsed < 500) {
                                return;
                            }

                            var hasFocus = stage.isFocused();
                            if (!hasFocus) {
                                regainedFocusCount++;
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
                            text.getField().requestFocus();
                            text.getField().end();
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
