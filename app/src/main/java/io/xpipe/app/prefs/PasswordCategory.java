package io.xpipe.app.prefs;

import atlantafx.base.theme.Styles;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.util.TerminalHelper;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.CommandControl;
import io.xpipe.core.store.LocalStore;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import lombok.SneakyThrows;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class PasswordCategory extends AppPrefsCategory {

    public PasswordCategory(AppPrefs prefs) {
        super(prefs);
    }

    @SneakyThrows
    public Category create() {
        var testPasswordManagerValue = new SimpleStringProperty();
        Runnable test = () -> {
            prefs.save();
            var cmd = prefs.passwordManagerString(testPasswordManagerValue.get());
            if (cmd == null) {
                return;
            }

            ThreadHelper.runFailableAsync(() -> {
                TerminalHelper.open(
                        "Password test",
                        new LocalStore()
                                .control()
                                .command(sc -> cmd
                                        + "\n"
                                        + sc.getShellDialect().getEchoCommand("Is this your password?", false))
                                .terminalExitMode(CommandControl.TerminalExitMode.KEEP_OPEN));
            });
        };

        var testPasswordManager = lazyNode(
                "passwordManagerCommandTest",
                new HorizontalComp(List.of(
                                new TextFieldComp(testPasswordManagerValue)
                                        .apply(struc -> struc.get().setPromptText("Test password key"))
                                        .styleClass(Styles.LEFT_PILL)
                                        .grow(false, true),
                                new ButtonComp(null, new FontIcon("mdi2p-play"), test)
                                        .styleClass(Styles.RIGHT_PILL)
                                        .grow(false, true)))
                        .padding(new Insets(15, 0, 0, 0))
                        .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                        .apply(struc -> struc.get().setFillHeight(true)),
                null);
        return Category.of(
                "passwordManager",
                Group.of(
                        Setting.of("passwordManagerCommand", prefs.passwordManagerCommand),
                        testPasswordManager));
    }
}
