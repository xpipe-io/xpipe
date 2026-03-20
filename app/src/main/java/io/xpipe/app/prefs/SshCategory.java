package io.xpipe.app.prefs;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.cred.CustomAgentStrategy;
import io.xpipe.app.cred.SshAgentTestComp;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.OsType;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;

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
    protected BaseRegionBuilder<?, ?> create() {
        var prefs = AppPrefs.get();
        var options = new OptionsBuilder().title("sshConfiguration");
        if (OsType.ofLocal() == OsType.WINDOWS) {
            options.addComp(prefs.getCustomOptions("x11WslInstance").buildComp());
        }

        var agentTest = new SshAgentTestComp(
                () -> {},
                new SimpleObjectProperty<>(CustomAgentStrategy.builder().build()));
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
            options.sub(new OptionsBuilder()
                    .nameAndDescription("sshAgentSocket")
                    .addComp(choice, prefs.sshAgentSocket)
                    .addComp(agentTest));
        }
        return options.buildComp();
    }
}
