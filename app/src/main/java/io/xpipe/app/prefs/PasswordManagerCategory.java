package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
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

import java.util.List;

public class PasswordManagerCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "passwordManager";
    }

    private void testPasswordManager(String key, StringProperty testPasswordManagerResult) {
        var prefs = AppPrefs.get();
        ThreadHelper.runFailableAsync(() -> {
            if (prefs.passwordManager.getValue() == null) {
                return;
            }

            var r = prefs.passwordManager.getValue().retrievePassword(key);
            Platform.runLater(() -> {
                testPasswordManagerResult.set(r);
                ThreadHelper.runAsync(() -> {
                    ThreadHelper.sleep(5000);
                    Platform.runLater(() -> {
                        testPasswordManagerResult.set(null);
                    });
                });
            });
        });
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var testPasswordManagerValue = new SimpleStringProperty();
        var testPasswordManagerResult = new SimpleStringProperty();

        var command = new IntegratedTextAreaComp(
                        prefs.passwordManagerCommand,
                        false,
                        "command",
                        new SimpleStringProperty(ProcessControlProvider.get()
                                .getEffectiveLocalDialect()
                                .getScriptFileEnding()))
                .apply(struc -> {
                    struc.getTextArea().setPromptText("mypassmgr get $KEY");
                })
                .disable(prefs.passwordManagerCommand.isNull())
                .hide(prefs.passwordManagerCommand.isNull())
                .minWidth(350)
                .minHeight(120);
        var choiceBuilder = OptionsChoiceBuilder.builder()
                .property(prefs.passwordManager)
                .subclasses(PasswordManager.getClasses())
                .transformer(entryComboBox -> {
                    var docsLinkButton =
                            new ButtonComp(AppI18n.observable("docs"), new FontIcon("mdi2h-help-circle-outline"), () -> {
                                var l = prefs.passwordManager.getValue().getDocsLink();
                                if (l != null) {
                                    Hyperlinks.open(l);
                                }
                            });
                    docsLinkButton.minWidth(Region.USE_PREF_SIZE);
                    docsLinkButton.disable(Bindings.createBooleanBinding(() -> {
                        return prefs.passwordManager.getValue().getDocsLink() == null;
                    }, prefs.passwordManager));

                    var hbox = new HBox(entryComboBox, docsLinkButton.createRegion());
                    HBox.setHgrow(entryComboBox, Priority.ALWAYS);
                    hbox.setSpacing(10);
                    return hbox;
        }).build();

        var top = choiceBuilder.build().buildComp();
        var choice = new VerticalComp(List.of(top, command)).apply(struc -> {
            struc.get().setAlignment(Pos.CENTER_LEFT);
            struc.get().setSpacing(10);
        });

        prefs.passwordManager.addListener((observable, oldValue, newValue) -> {
            System.out.println(newValue);
        });

        var testInput = new HorizontalComp(List.<Comp<?>>of(
                new TextFieldComp(testPasswordManagerValue)
                        .apply(struc -> struc.get().setPromptText("Enter password key"))
                        .styleClass(Styles.LEFT_PILL)
                        .prefWidth(400)
                        .apply(struc -> struc.get().setOnKeyPressed(event -> {
                            if (event.getCode() == KeyCode.ENTER) {
                                testPasswordManager(testPasswordManagerValue.get(), testPasswordManagerResult);
                                event.consume();
                            }
                        })),
                new ButtonComp(null, new FontIcon("mdi2p-play"), () -> {
                    testPasswordManager(testPasswordManagerValue.get(), testPasswordManagerResult);
                }).styleClass(Styles.RIGHT_PILL)));
        testInput.apply(struc -> {
            struc.get().setFillHeight(true);
            var first = ((Region) struc.get().getChildren().get(0));
            var second = ((Region) struc.get().getChildren().get(1));
            second.minHeightProperty().bind(first.heightProperty());
            second.maxHeightProperty().bind(first.heightProperty());
            second.prefHeightProperty().bind(first.heightProperty());
        });

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
                        .addComp(testPasswordManager))
                .buildComp();
    }
}
