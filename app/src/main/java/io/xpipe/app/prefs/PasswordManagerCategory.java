package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.password.PasswordManager;
import io.xpipe.app.util.*;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Duration;
import java.util.List;

public class PasswordManagerCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "passwordManager";
    }

    private void testPasswordManager(String key, StringProperty testPasswordManagerResult) {
        var prefs = AppPrefs.get();
        ThreadHelper.runFailableAsync(() -> {
            if (prefs.passwordManager.getValue() == null || key == null) {
                return;
            }

            Platform.runLater(() -> {
                testPasswordManagerResult.set(AppI18n.get("querying"));
            });

            var r = prefs.passwordManager.getValue().retrievePassword(key);
            Platform.runLater(() -> {
                testPasswordManagerResult.set(r != null ? AppI18n.get("retrievedPassword", r) : null);
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

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var testPasswordManagerValue = new SimpleStringProperty();
        var testPasswordManagerResult = new SimpleStringProperty();

        var choiceBuilder = OptionsChoiceBuilder.builder()
                .property(prefs.passwordManager)
                .subclasses(PasswordManager.getClasses())
                .allowNull(true)
                .transformer(entryComboBox -> {
                    var docsLinkButton = new ButtonComp(
                            AppI18n.observable("docs"), new FontIcon("mdi2h-help-circle-outline"), () -> {
                                Hyperlinks.open(DocumentationLink.PASSWORD_MANAGER.getLink());
                            });
                    docsLinkButton.minWidth(Region.USE_PREF_SIZE);

                    var hbox = new HBox(entryComboBox, docsLinkButton.createRegion());
                    HBox.setHgrow(entryComboBox, Priority.ALWAYS);
                    hbox.setSpacing(10);
                    hbox.setMaxWidth(getCompWidth());
                    return hbox;
                })
                .build();
        var choice = choiceBuilder.build().buildComp();

        var testInput = new HorizontalComp(List.<Comp<?>>of(
                new TextFieldComp(testPasswordManagerValue)
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
                                testPasswordManager(testPasswordManagerValue.get(), testPasswordManagerResult);
                                event.consume();
                            }
                        })),
                new ButtonComp(null, new FontIcon("mdi2p-play"), () -> {
                            testPasswordManager(testPasswordManagerValue.get(), testPasswordManagerResult);
                        })
                        .styleClass(Styles.RIGHT_PILL)));
        testInput.apply(struc -> {
            struc.get().setFillHeight(true);
            var first = ((Region) struc.get().getChildren().get(0));
            var second = ((Region) struc.get().getChildren().get(1));
            second.minHeightProperty().bind(first.heightProperty());
            second.maxHeightProperty().bind(first.heightProperty());
            second.prefHeightProperty().bind(first.heightProperty());
        });
        testInput.maxWidth(getCompWidth());
        testInput.hgrow();

        var testPasswordManager = new HorizontalComp(List.of(
                        testInput, Comp.hspacer(25), new LabelComp(testPasswordManagerResult).apply(struc -> struc.get()
                                .setOpacity(0.8))))
                .padding(new Insets(10, 0, 0, 0))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .apply(struc -> struc.get().setFillHeight(true));

        return new OptionsBuilder()
                .addTitle("passwordManager")
                .sub(new OptionsBuilder()
                        .pref(prefs.passwordManager)
                        .addComp(choice)
                        .nameAndDescription("passwordManagerCommandTest")
                        .addComp(testPasswordManager)
                        .hide(BindingsHelper.map(prefs.passwordManager, p -> p == null)))
                .buildComp();
    }
}
