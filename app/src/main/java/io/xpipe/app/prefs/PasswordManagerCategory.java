package io.xpipe.app.prefs;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.CommandControl;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

public class PasswordManagerCategory extends AppPrefsCategory {

    @Value
    private static class Choice {
        String id;
        String template;
        ExternalPasswordManager passwordManager;
    }

    @Override
    protected String getId() {
        return "passwordManager";
    }

    private Comp<?> createTemplateChoice() {
        return Comp.of(() -> {
            var cb = new MenuButton();
            cb.textProperty().bind(AppI18n.observable("templates"));
            ExternalPasswordManagerTemplate.ALL.forEach(e -> {
                var m = new MenuItem(
                        e.toTranslatedString().getValue());
                m.setOnAction(event -> {
                    AppPrefs.get().passwordManagerCommand.set(e.getTemplate());
                    event.consume();
                });
                cb.getItems().add(m);
            });
            return cb;
        });
    }

    @Override
    protected Comp<?> create() {
        var choices = new ArrayList<Choice>();
        ExternalPasswordManagerTemplate.ALL.forEach(externalPasswordManagerTemplate -> {
            choices.add(new Choice(externalPasswordManagerTemplate.getId(), externalPasswordManagerTemplate.getTemplate(), null));
        });
        ExternalPasswordManager.ALL.stream().filter(externalPasswordManager -> externalPasswordManager != ExternalPasswordManager.COMMAND).forEach(externalPasswordManager -> {
            choices.add(new Choice(externalPasswordManager.getId(), null, externalPasswordManager));
        });

        var prefs = AppPrefs.get();
        var testPasswordManagerValue = new SimpleStringProperty();
        Runnable test = () -> {
            var cmd = prefs.passwordManagerString(testPasswordManagerValue.get());
            if (cmd == null) {
                return;
            }

            ThreadHelper.runFailableAsync(() -> {
                TerminalLauncher.open(
                        "Password test",
                        new LocalStore()
                                .control()
                                .command(CommandBuilder.ofFunction(sc -> cmd
                                        + "\n"
                                        + sc.getShellDialect().getEchoCommand("Is this your password?", false)))
                                .terminalExitMode(CommandControl.TerminalExitMode.KEEP_OPEN));
            });
        };

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
                .minWidth(350)
                .minHeight(120);
        var templates = Comp.of(() -> {
            var cb = new MenuButton();
            cb.textProperty().bind(BindingsHelper.flatMap(prefs.passwordManager,externalPasswordManager -> {
                return externalPasswordManager != null ? AppI18n.observable(externalPasswordManager.getId()) : AppI18n.observable("templates");
            }));
            choices.forEach(e -> {
                var m = new MenuItem();
                m.textProperty().bind(AppI18n.observable(e.getId()));
                m.setOnAction(event -> {
                    AppPrefs.get().passwordManagerCommand.set(e.getTemplate());
                    AppPrefs.get().passwordManager.setValue(e.getPasswordManager());
                    event.consume();
                });
                cb.getItems().add(m);
            });
            return cb;
        });
        var choice = new VerticalComp(List.of(templates, command)).apply(struc -> {
            struc.get().setAlignment(Pos.CENTER_LEFT);
            struc.get().setSpacing(10);
        });

        var testPasswordManager = new HorizontalComp(List.of(
                        new TextFieldComp(testPasswordManagerValue)
                                .apply(struc -> struc.get().setPromptText("Enter password key"))
                                .styleClass(Styles.LEFT_PILL)
                                .grow(false, true),
                        new ButtonComp(null, new FontIcon("mdi2p-play"), test)
                                .styleClass(Styles.RIGHT_PILL)
                                .grow(false, true)))
                .padding(new Insets(15, 0, 0, 0))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .apply(struc -> struc.get().setFillHeight(true));
        return new OptionsBuilder()
                .addTitle("passwordManager")
                .sub(new OptionsBuilder()
                        .nameAndDescription("passwordManagerCommand")
                        .addComp(choice)
                        .nameAndDescription("passwordManagerCommandTest")
                        .addComp(testPasswordManager))
                .buildComp();
    }
}
