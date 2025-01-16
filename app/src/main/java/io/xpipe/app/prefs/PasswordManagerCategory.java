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
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

public class PasswordManagerCategory extends AppPrefsCategory {

    @Value
    private static class Choice {
        String id;
        String template;
        String docsLink;
        ExternalPasswordManager passwordManager;
    }

    @Override
    protected String getId() {
        return "passwordManager";
    }

    @Override
    protected Comp<?> create() {
        var choices = new ArrayList<Choice>();
        ExternalPasswordManagerTemplate.ALL.forEach(externalPasswordManagerTemplate -> {
            choices.add(new Choice(
                    externalPasswordManagerTemplate.getId(),
                    externalPasswordManagerTemplate.getTemplate(),
                    externalPasswordManagerTemplate.getDocsLink(),
                    ExternalPasswordManager.COMMAND));
        });
        ExternalPasswordManager.ALL.stream()
                .filter(externalPasswordManager -> externalPasswordManager != ExternalPasswordManager.COMMAND)
                .forEach(externalPasswordManager -> {
                    choices.add(new Choice(
                            externalPasswordManager.getId(),
                            null,
                            externalPasswordManager.getDocsLink(),
                            externalPasswordManager));
                });

        var prefs = AppPrefs.get();
        var testPasswordManagerValue = new SimpleStringProperty();
        var testPasswordManagerResult = new SimpleStringProperty();
        Runnable test = () -> {
            ThreadHelper.runFailableAsync(() -> {
                if (prefs.passwordManager.getValue() == null) {
                    return;
                }

                var r = prefs.passwordManager.getValue().retrievePassword(testPasswordManagerValue.get());
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
        };

        var docsLinkProperty = new SimpleStringProperty();
        var docsLinkButton =
                new ButtonComp(AppI18n.observable("documentation"), new FontIcon("mdi2h-help-circle-outline"), () -> {
                    var l = docsLinkProperty.get();
                    if (l != null) {
                        Hyperlinks.open(l);
                    }
                });
        docsLinkButton.disable(docsLinkProperty.isNull());

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
        var templates = Comp.of(() -> {
            var cb = new MenuButton();
            cb.textProperty().bind(BindingsHelper.flatMap(prefs.passwordManager, externalPasswordManager -> {
                return externalPasswordManager != null
                        ? AppI18n.observable(externalPasswordManager.getId())
                        : AppI18n.observable("selectType");
            }));
            choices.forEach(e -> {
                var m = new MenuItem();
                m.textProperty().bind(AppI18n.observable(e.getId()));
                m.setOnAction(event -> {
                    AppPrefs.get().passwordManagerCommand.set(e.getTemplate());
                    AppPrefs.get().passwordManager.setValue(e.getPasswordManager());
                    docsLinkProperty.set(e.getDocsLink());
                    event.consume();
                });
                cb.getItems().add(m);
            });
            return cb;
        });
        var top = new HorizontalComp(List.of(templates, docsLinkButton))
                .spacing(10)
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));
        var choice = new VerticalComp(List.of(top, command)).apply(struc -> {
            struc.get().setAlignment(Pos.CENTER_LEFT);
            struc.get().setSpacing(10);
        });

        var testInput = new HorizontalComp(List.<Comp<?>>of(
                new TextFieldComp(testPasswordManagerValue)
                        .apply(struc -> struc.get().setPromptText("Enter password key"))
                        .styleClass(Styles.LEFT_PILL)
                        .apply(struc -> struc.get().setOnKeyPressed(event -> {
                            if (event.getCode() == KeyCode.ENTER) {
                                test.run();
                                event.consume();
                            }
                        })),
                new ButtonComp(null, new FontIcon("mdi2p-play"), test).styleClass(Styles.RIGHT_PILL)));
        testInput.apply(struc -> {
            var first = ((Region) struc.get().getChildren().get(0));
            var second = ((Region) struc.get().getChildren().get(1));
            second.prefHeightProperty().bind(first.heightProperty());
        });

        var testPasswordManager = new HorizontalComp(List.of(
                        testInput, Comp.hspacer(25), new LabelComp(testPasswordManagerResult).apply(struc -> struc.get()
                                .setOpacity(0.5))))
                .padding(new Insets(10, 0, 0, 0))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .apply(struc -> struc.get().setFillHeight(true));
        return new OptionsBuilder()
                .addTitle("passwordManager")
                .sub(new OptionsBuilder()
                        .pref(prefs.passwordManagerCommand)
                        .addComp(choice)
                        .nameAndDescription("passwordManagerCommandTest")
                        .addComp(testPasswordManager))
                .buildComp();
    }
}
