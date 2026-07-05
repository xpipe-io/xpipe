package io.xpipe.app.prefs;

import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.App;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.cred.SshAgentKeyList;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.pwman.PasswordManager;
import io.xpipe.app.pwman.PasswordManagerKeyList;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import javafx.scene.layout.VBox;
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

        var listButton = new ButtonComp(null, new LabelGraphic.IconGraphic("mdi2m-magnify-scan"), null);
        listButton.enable(PasswordManagerKeyList.isSupported());
        listButton.apply(struc -> {
            struc.setOnAction(event -> {
                struc.setDisable(true);
                ThreadHelper.runFailableAsync(() -> {
                    var list = PasswordManagerKeyList.queryList(false);
                    Platform.runLater(() -> {
                        struc.setDisable(false);

                        var popover = new Popover();
                        popover.setArrowLocation(Popover.ArrowLocation.TOP_CENTER);

                        if (list.size() > 0) {
                            var content = new VBox();
                            content.setPadding(new Insets(10));
                            content.setFillWidth(true);

                            var header = new Label(AppI18n.get("passwordManagerHasKeys"));
                            header.getStyleClass().add(Styles.TEXT_BOLD);
                            header.setPadding(new Insets(0, 0, 8, 8));

                            var refresh = new IconButtonComp("mdi2r-refresh", () -> {
                                struc.setDisable(true);
                                popover.hide();
                                ThreadHelper.runAsync(() -> {
                                    PasswordManagerKeyList.queryList(true);
                                    Platform.runLater(() -> {
                                        struc.setDisable(false);
                                        struc.fire();
                                    });
                                });
                            })
                                    .maxHeight(100)
                                    .build();

                            var headerBar = new HBox(header, new Spacer(), refresh);
                            headerBar.setAlignment(Pos.CENTER_LEFT);
                            headerBar.setFillHeight(true);

                            content.getChildren().add(headerBar);

                            var entries = new VBox();
                            entries.setFillWidth(true);
                            for (var entry : list) {
                                var buttonName = entry.getKey();
                                var entryButton = new Button(buttonName);
                                entryButton.setMaxWidth(400);
                                entryButton.getStyleClass().add(Styles.FLAT);
                                entryButton.setOnAction(e -> {
                                    value.setValue(entry.getKey());
                                    popover.hide();
                                    e.consume();
                                });
                                entryButton.setMinWidth(400);
                                entryButton.setAlignment(Pos.CENTER_LEFT);
                                entryButton.setMnemonicParsing(false);
                                entries.getChildren().add(entryButton);
                            }

                            var sp = new ScrollPane(entries);
                            sp.setFitToWidth(true);
                            sp.setMaxHeight(350);
                            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                            content.getChildren().add(sp);
                            popover.setContentNode(content);
                        } else {
                            var content = new Label(AppI18n.get("passwordManagerNoKeys"));
                            content.setPadding(new Insets(10));
                            popover.setContentNode(content);
                        }

                        var target = struc.getParent().getChildrenUnmodifiable().getFirst();
                        if (!popover.isShowing() && target.getScene() != null) {
                            popover.show(target);
                        }
                    });
                });
                event.consume();
            });
        });

        var settingsButton = new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
            AppPrefs.get().selectCategory("passwordManager");
            App.getApp().getStage().requestFocus();
        });

        var l = new ArrayList<BaseRegionBuilder<?, ?>>();
        l.add(field);
        l.add(listButton);
        if (showSettings) {
            l.add(settingsButton);
        }
        var fieldBox = new InputGroupComp(l);
        fieldBox.setMainReference(field);

        var testButton = new ButtonComp(AppI18n.observable("test"), new FontIcon("mdi2p-play"), () -> {
            testPasswordManager(value.getValue(), testPasswordManagerResult);
        });
        testButton.padding(new Insets(6, 9, 6, 9));
        testButton.disable(BindingsHelper.mapBoolean(value, v -> v == null));

        var testRow = new HorizontalComp(
                        List.of(testButton, new LabelComp(testPasswordManagerResult).apply(struc -> struc.setOpacity(0.8))))
                .apply(struc -> struc.setAlignment(Pos.CENTER_LEFT))
                .apply(struc -> struc.setFillHeight(true));

        var vbox = new VerticalComp(List.of(fieldBox, testRow));
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
