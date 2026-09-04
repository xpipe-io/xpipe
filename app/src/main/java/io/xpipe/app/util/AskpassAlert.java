package io.xpipe.app.util;

import io.xpipe.app.comp.base.SecretFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.core.window.AppSideWindow;
import io.xpipe.app.secret.InPlaceSecretValue;
import io.xpipe.app.secret.SecretManager;
import io.xpipe.app.secret.SecretQueryResult;
import io.xpipe.app.secret.SecretQueryState;

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
        if (AppOperationMode.isInShutdown()) {
            return new SecretQueryResult(null, SecretQueryState.CANCELLED);
        }

        prompt = prompt.strip();
        if (prompt.endsWith(":") || prompt.endsWith("?")) {
            prompt = prompt.substring(0, prompt.length() - 1);
        }

        var lines = prompt.lines().count();
        if (lines > 13) {
            prompt = String.join("\n", prompt.lines().toList().subList(0, 13)) + "\n...";
            lines = 14;
        }

        var prop = new SimpleObjectProperty<>(secretValue);

        var finalPrompt = prompt;
        var finalLines = lines;

        var r = AppSideWindow.showBlockingAlert(alert -> {
                    alert.initModality(Modality.NONE);
                    alert.setTitle(AppI18n.get("askpassAlertTitle"));
                    alert.setHeaderText(finalPrompt);
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                    alert.getButtonTypes().setAll(ButtonType.OK);

                    ButtonBar buttonBar = (ButtonBar) alert.getDialogPane().lookup(".button-bar");
                    buttonBar.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);

                    if (finalLines > 3) {
                        // Title bar + button bar + padding + text field + separator + lines
                        alert.setHeight(30 + 50 + 40 + 40 + 20 + (finalLines * 30));
                    }

                    // Link to help page for double prompt
                    if (SecretManager.disableCachingForPrompt(finalPrompt)) {
                        var type = new ButtonType("Help", ButtonBar.ButtonData.HELP);
                        alert.getButtonTypes().addFirst(type);
                        var button = alert.getDialogPane().lookupButton(type);
                        button.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                            DocumentationLink.DOUBLE_PROMPT.open();
                            event.consume();
                        });
                    }

                    var text = new SecretFieldComp(prop, false).buildStructure();
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

                    alert.setOnHiding(e -> {
                        anim.stop();
                        e.consume();
                    });
                })
                .filter(b -> b.getButtonData().isDefaultButton())
                .map(t -> {
                    return prop.getValue() != null ? prop.getValue() : InPlaceSecretValue.of("");
                })
                .orElse(null);

        if (r != null && r.getSecret().length == 0) {
            r = null;
        }

        return new SecretQueryResult(r, r == null ? SecretQueryState.CANCELLED : SecretQueryState.NORMAL);
    }
}
