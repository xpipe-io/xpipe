package io.xpipe.app.prefs;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.App;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
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

    private final Property<String> value;
    private final boolean handleEnter;
    private final boolean showName;
    private final boolean showSettings;
    private final AtomicInteger counter = new AtomicInteger(0);

    public PasswordManagerTestComp(boolean handleEnter) {
        this(new SimpleStringProperty(), handleEnter, false, false);
    }

    public PasswordManagerTestComp(
            Property<String> value, boolean handleEnter, boolean showName, boolean showSettings) {
        this.value = value;
        this.handleEnter = handleEnter;
        this.showName = showName;
        this.showSettings = showSettings;
    }

    @Override
    protected Region createSimple() {
        var prefs = AppPrefs.get();
        var testPasswordManagerResult = new SimpleStringProperty();

        var field = new TextFieldComp(value)
                .apply(struc -> struc.promptTextProperty()
                        .bind(Bindings.createStringBinding(
                                () -> {
                                    var p = prefs.passwordManager.getValue();
                                    return p != null
                                            ? (showName ? p.getDisplayName() + " - " : "") + p.getKeyPlaceholder()
                                            : "?";
                                },
                                prefs.passwordManager,
                                AppI18n.activeLanguage())))
                .hgrow();
        if (handleEnter) {
            field.apply(struc -> struc.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    testPasswordManager(value.getValue(), testPasswordManagerResult);
                    event.consume();
                }
            }));
        }

        var settingsButton = new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
            AppPrefs.get().selectCategory("passwordManager");
            App.getApp().getStage().requestFocus();
        });
        var fieldBox = new InputGroupComp(List.of(field, settingsButton));
        fieldBox.setMainReference(field);

        var button = new ButtonComp(AppI18n.observable("test"), new FontIcon("mdi2p-play"), () -> {
            testPasswordManager(value.getValue(), testPasswordManagerResult);
        });
        button.padding(new Insets(6, 9, 6, 9));
        button.disable(BindingsHelper.mapBoolean(value, v -> v == null));

        var testRow = new HorizontalComp(
                        List.of(button, new LabelComp(testPasswordManagerResult).apply(struc -> struc.setOpacity(0.8))))
                .apply(struc -> struc.setAlignment(Pos.CENTER_LEFT))
                .apply(struc -> struc.setFillHeight(true));

        var vbox = new VerticalComp(List.of(showSettings ? fieldBox : field, testRow));
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

            if (prefs.passwordManager.getValue() != null) {
                if (r.getSshKey() != null) {
                    var noRetrievalStrategy = !prefs.passwordManager.getValue().getKeyConfiguration().useInline() &&
                            !prefs.passwordManager.getValue().getKeyConfiguration().useAgent();
                    elements.add(AppI18n.get(noRetrievalStrategy ? "sshKeyRetrievedMissingStrategy" : "sshKeyRetrieved"));
                }
            }

            var formatted = String.join(" + ", elements);
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
