package io.xpipe.app.prefs;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;

import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PasswordManagerTestComp extends SimpleRegionBuilder {

    private final StringProperty value;
    private final boolean handleEnter;
    private final AtomicInteger counter = new AtomicInteger(0);

    public PasswordManagerTestComp(boolean handleEnter) {
        this(new SimpleStringProperty(), handleEnter);
    }

    public PasswordManagerTestComp(StringProperty value, boolean handleEnter) {
        this.value = value;
        this.handleEnter = handleEnter;
    }

    @Override
    protected Region createSimple() {
        var prefs = AppPrefs.get();
        var testPasswordManagerResult = new SimpleStringProperty();

        var field = new TextFieldComp(value)
                .apply(struc -> struc.promptTextProperty()
                        .bind(Bindings.createStringBinding(
                                () -> {
                                    return prefs.passwordManager.getValue() != null
                                            ? prefs.passwordManager.getValue().getKeyPlaceholder()
                                            : "?";
                                },
                                prefs.passwordManager)))
                .hgrow();
        if (handleEnter) {
            field.apply(struc -> struc.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    testPasswordManager(value.get(), testPasswordManagerResult);
                    event.consume();
                }
            }));
        }

        var button = new ButtonComp(AppI18n.observable("test"), new FontIcon("mdi2p-play"), () -> {
            testPasswordManager(value.get(), testPasswordManagerResult);
        });
        button.padding(new Insets(6, 9, 6, 9));
        button.disable(value.isNull());

        var testRow = new HorizontalComp(
                        List.of(button, new LabelComp(testPasswordManagerResult).apply(struc -> struc.setOpacity(0.8))))
                .apply(struc -> struc.setAlignment(Pos.CENTER_LEFT))
                .apply(struc -> struc.setFillHeight(true));

        var vbox = new VerticalComp(List.of(field, testRow));
        vbox.spacing(6);
        vbox.apply(r -> r.focusedProperty().subscribe(focus -> {
            if (focus) {
                r.getChildren().getFirst().requestFocus();
            }
        }));
        return vbox.build();
    }

    private void testPasswordManager(String key, StringProperty testPasswordManagerResult) {
        var currentIndex = counter.incrementAndGet();
        var prefs = AppPrefs.get();
        ThreadHelper.runFailableAsync(() -> {
            if (prefs.passwordManager.getValue() == null || key == null) {
                return;
            }

            Platform.runLater(() -> {
                testPasswordManagerResult.set("    " + AppI18n.get("querying"));
            });

            var r = prefs.passwordManager.getValue().query(key);
            if (r == null) {
                Platform.runLater(() -> {
                    testPasswordManagerResult.set("    " + AppI18n.get("queryFailed"));
                });
                GlobalTimer.delay(
                        () -> {
                            Platform.runLater(() -> {
                                if (counter.get() == currentIndex) {
                                    testPasswordManagerResult.set(null);
                                }
                            });
                        },
                        Duration.ofSeconds(5));
                return;
            }

            List<String> elements = new ArrayList<>();
            if (r.getCredentials() != null) {
                elements.add(
                        r.getCredentials().getUsername() != null
                                ? r.getCredentials().getUsername()
                                : "<no user>");
                if (r.getCredentials().getPassword() != null) {
                    var secret = r.getCredentials().getPassword().getSecretValue();
                    var secretFormatted =
                            secret.length() > 4 ? secret.substring(0, 4) + "*".repeat(secret.length() - 4) : secret;
                    elements.add(secretFormatted);
                } else {
                    elements.add("<no password>");
                }
            } else {
                elements.add("<no credentials>");
            }

            if (prefs.passwordManager.getValue() != null
                    && prefs.passwordManager.getValue().getKeyConfiguration().useInline()) {
                if (r.getSshKey() != null) {
                    elements.add(AppI18n.get("sshKey"));
                }
            }

            var formatted = String.join(" / ", elements);
            Platform.runLater(() -> {
                testPasswordManagerResult.set("    " + AppI18n.get("retrievedPassword", formatted));
            });
            GlobalTimer.delay(
                    () -> {
                        Platform.runLater(() -> {
                            if (counter.get() == currentIndex) {
                                testPasswordManagerResult.set(null);
                            }
                        });
                    },
                    Duration.ofSeconds(5));
        });
    }
}
