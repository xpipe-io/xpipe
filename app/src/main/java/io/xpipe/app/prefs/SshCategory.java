package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorage;
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
        if (OsType.ofLocal() == OsType.WINDOWS) {
            options.addComp(prefs.getCustomOptions("x11WslInstance").buildComp());
        }
        if (OsType.ofLocal() != OsType.WINDOWS) {
            var choice = new ContextualFileReferenceChoiceComp(
                    new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()),
                    prefs.sshAgentSocket,
                    null,
                    List.of(),
                    e -> e.equals(DataStorage.get().local()),
                    false);
            choice.setPrompt(prefs.defaultSshAgentSocket);
            choice.maxWidth(600);
            options.sub(
                    new OptionsBuilder().nameAndDescription("sshAgentSocket").addComp(choice, prefs.sshAgentSocket));
        }
        return options.buildComp();
    }
}
