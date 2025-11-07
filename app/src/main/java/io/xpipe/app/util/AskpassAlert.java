package io.xpipe.app.util;

import io.xpipe.app.comp.base.SecretFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppSideWindow;
import io.xpipe.app.secret.SecretManager;
import io.xpipe.app.secret.SecretQueryResult;
import io.xpipe.app.secret.SecretQueryState;
import io.xpipe.core.InPlaceSecretValue;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class AskpassAlert {

    public static SecretQueryResult queryRaw(String prompt, InPlaceSecretValue secretValue, boolean stealFocus) {
        var prop = new SimpleObjectProperty<>(secretValue);
        var r = AppSideWindow.showBlockingAlert(alert -> {
                    alert.initModality(Modality.NONE);
                    alert.setTitle(AppI18n.get("askpassAlertTitle"));
                    alert.setHeaderText(prompt);
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);

                    // Link to help page for double prompt
                    if (SecretManager.disableCachingForPrompt(prompt)) {
                        var type = new ButtonType("Help", ButtonBar.ButtonData.HELP);
                        alert.getButtonTypes().add(type);
                        var button = alert.getDialogPane().lookupButton(type);
                        button.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                            DocumentationLink.DOUBLE_PROMPT.open();
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

                            if (regainedFocusCount >= 3) {
                                return;
                            }

                            var hasInternalFocus = Window.getWindows().stream()
                                    .filter(window -> window != stage)
                                    .anyMatch(window -> window instanceof Stage s
                                            && s.focusedProperty().get());
                            if (hasInternalFocus) {
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
                        if (stealFocus) {
                            anim.start();
                        }
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
        return new SecretQueryResult(r, r == null ? SecretQueryState.CANCELLED : SecretQueryState.NORMAL);
    }
}
