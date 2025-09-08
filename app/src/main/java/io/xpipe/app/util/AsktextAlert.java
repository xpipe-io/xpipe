package io.xpipe.app.util;

import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppSideWindow;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.Optional;

public class AsktextAlert {

    public static Optional<String> query(String prompt, String value) {
        var prop = new SimpleObjectProperty<String>(value);
        var r = AppSideWindow.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("asktextAlertTitle"));
                    alert.setHeaderText(prompt);
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);

                    var text = new TextFieldComp(prop, false).createStructure();
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
                            text.get().requestFocus();
                            text.get().end();
                        });
                        event.consume();
                    });

                    alert.setOnHiding(event -> {
                        anim.stop();
                    });
                })
                .filter(b -> b.getButtonData().isDefaultButton())
                .map(t -> {
                    return prop.getValue() != null ? prop.getValue() : null;
                });
        return r;
    }
}
