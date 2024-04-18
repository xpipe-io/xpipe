package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.CommandControl;
import io.xpipe.core.store.LocalStore;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class PasswordManagerCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "passwordManager";
    }

    private Comp<?> createTemplateChoice() {
        return Comp.of(() -> {
            var cb = new MenuButton();
            cb.textProperty().bind(AppI18n.observable("templates"));
            ExternalPasswordManager.ALL.forEach(externalPasswordManager -> {
                var m = new MenuItem(
                        externalPasswordManager.toTranslatedString().getValue());
                m.setOnAction(event -> {
                    AppPrefs.get().passwordManagerCommand.set(externalPasswordManager.getTemplate());
                    event.consume();
                });
                cb.getItems().add(m);
            });
            return cb;
        });
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var testPasswordManagerValue = new SimpleStringProperty();
        Runnable test = () -> {
            prefs.save();
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

        var c = new TextFieldComp(prefs.passwordManagerCommand, true)
                .apply(struc -> struc.get().setPromptText("mypassmgr get $KEY"))
                .minWidth(350);
        var visit = createTemplateChoice();
        var choice = new HorizontalComp(List.of(c, visit)).apply(struc -> {
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
