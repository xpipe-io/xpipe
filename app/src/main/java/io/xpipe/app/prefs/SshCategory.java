package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.OsType;

import javafx.beans.property.ReadOnlyObjectWrapper;

import java.util.List;

public class SshCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "ssh";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-console-network-outline");
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var options = new OptionsBuilder().addTitle("sshConfiguration");
        if (OsType.getLocal() == OsType.WINDOWS) {
            options.addComp(prefs.getCustomOptions("x11WslInstance").buildComp());
        }
        if (OsType.getLocal() != OsType.WINDOWS) {
            var choice = new ContextualFileReferenceChoiceComp(
                    new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()),
                    prefs.sshAgentSocket,
                    null,
                    List.of());
            choice.setPrompt(prefs.defaultSshAgentSocket);
            choice.maxWidth(600);
            options.sub(
                    new OptionsBuilder().nameAndDescription("sshAgentSocket").addComp(choice, prefs.sshAgentSocket));
        }
        options.sub(new OptionsBuilder().pref(prefs.alwaysShowSshMotd).addToggle(prefs.alwaysShowSshMotd));
        return options.buildComp();
    }
}
