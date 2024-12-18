package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.ProcessOutputException;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class DeveloperCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "developer";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var localCommand = new SimpleStringProperty();
        Runnable test = () -> {
            var cmd = localCommand.get();
            if (cmd == null) {
                return;
            }

            ThreadHelper.runFailableAsync(() -> {
                try {
                    TrackEvent.info(LocalShell.getShell().executeSimpleStringCommand(cmd));
                } catch (ProcessOutputException ex) {
                    TrackEvent.error(ex.getOutput());
                }
            });
        };

        var runLocalCommand = new HorizontalComp(List.of(
                        new TextFieldComp(localCommand)
                                .apply(struc -> struc.get().setPromptText("Local command"))
                                .styleClass(Styles.LEFT_PILL)
                                .grow(false, true),
                        new ButtonComp(null, new FontIcon("mdi2p-play"), test)
                                .styleClass(Styles.RIGHT_PILL)
                                .grow(false, true)))
                .padding(new Insets(15, 0, 0, 0))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .apply(struc -> struc.get().setFillHeight(true));
        var sub = new OptionsBuilder()
                .nameAndDescription("developerDisableUpdateVersionCheck")
                .addToggle(prefs.developerDisableUpdateVersionCheck);
        if (AppProperties.get().isDevelopmentEnvironment()) {
            sub.nameAndDescription("developerForceSshTty").addToggle(prefs.developerForceSshTty);
        }
        sub.nameAndDescription("shellCommandTest").addComp(runLocalCommand);
        return new OptionsBuilder().addTitle("developer").sub(sub).buildComp();
    }
}
