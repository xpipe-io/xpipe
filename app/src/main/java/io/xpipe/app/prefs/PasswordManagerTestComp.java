package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Duration;
import java.util.List;

public class PasswordManagerTestComp extends SimpleComp {

    private final StringProperty value;

    public PasswordManagerTestComp(StringProperty value) {
        this.value = value;
    }

    @Override
    protected Region createSimple() {
        var prefs = AppPrefs.get();
        var testPasswordManagerResult = new SimpleStringProperty();

        var testInput = new HorizontalComp(List.<Comp<?>>of(
                new TextFieldComp(value)
                        .apply(struc -> struc.get()
                                .promptTextProperty()
                                .bind(Bindings.createStringBinding(
                                        () -> {
                                            return prefs.passwordManager.getValue() != null
                                                    ? prefs.passwordManager
                                                            .getValue()
                                                            .getKeyPlaceholder()
                                                    : "?";
                                        },
                                        prefs.passwordManager)))
                        .styleClass(Styles.LEFT_PILL)
                        .hgrow()
                        .apply(struc -> struc.get().setOnKeyPressed(event -> {
                            if (event.getCode() == KeyCode.ENTER) {
                                testPasswordManager(value.get(), testPasswordManagerResult);
                                event.consume();
                            }
                        })),
                new ButtonComp(null, new FontIcon("mdi2p-play"), () -> {
                            testPasswordManager(value.get(), testPasswordManagerResult);
                        })
                        .tooltip(AppI18n.observable("test"))
                        .styleClass(Styles.RIGHT_PILL)));
        testInput.apply(struc -> {
            struc.get().setFillHeight(true);
            var first = ((Region) struc.get().getChildren().get(0));
            var second = ((Region) struc.get().getChildren().get(1));
            second.minHeightProperty().bind(first.heightProperty());
            second.maxHeightProperty().bind(first.heightProperty());
            second.prefHeightProperty().bind(first.heightProperty());
        });
        testInput.hgrow();

        var testPasswordManager = new HorizontalComp(
                        List.of(testInput, new LabelComp(testPasswordManagerResult).apply(struc -> struc.get()
                                .setOpacity(0.8))))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .apply(struc -> struc.get().setFillHeight(true));
        return testPasswordManager.createRegion();
    }

    private void testPasswordManager(String key, StringProperty testPasswordManagerResult) {
        var prefs = AppPrefs.get();
        ThreadHelper.runFailableAsync(() -> {
            if (prefs.passwordManager.getValue() == null || key == null) {
                return;
            }

            Platform.runLater(() -> {
                testPasswordManagerResult.set("    " + AppI18n.get("querying"));
            });

            var r = prefs.passwordManager.getValue().retrieveCredentials(key);
            if (r == null) {
                Platform.runLater(() -> {
                    testPasswordManagerResult.set(null);
                });
                return;
            }

            var pass = r.getPassword() != null ? r.getPassword().getSecretValue() : "?";
            var format = (r.getUsername() != null ? r.getUsername() + " [" + pass + "]" : pass);
            Platform.runLater(() -> {
                testPasswordManagerResult.set("    " + AppI18n.get("retrievedPassword", format));
            });
            GlobalTimer.delay(
                    () -> {
                        Platform.runLater(() -> {
                            testPasswordManagerResult.set(null);
                        });
                    },
                    Duration.ofSeconds(5));
        });
    }
}
