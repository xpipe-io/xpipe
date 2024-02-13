package io.xpipe.app.prefs;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.ButtonComp;
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
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class PasswordManagerCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "passwordManager";
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
        return new OptionsBuilder().addTitle("passwordManager").sub(new OptionsBuilder()
                .nameAndDescription("passwordManagerCommand")
                .addComp(new TextFieldComp(prefs.passwordManagerCommand, true).apply(struc -> struc.get().setPromptText("mypassmgr get $KEY")))
                .nameAndDescription("passwordManagerCommandTest")
                .addComp(testPasswordManager)).buildComp();
    }
}
