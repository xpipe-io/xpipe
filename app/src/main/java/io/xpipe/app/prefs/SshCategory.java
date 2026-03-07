package io.xpipe.app.prefs;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.cred.CustomAgentStrategy;
import io.xpipe.app.cred.SshAgentTestComp;
import io.xpipe.app.cred.SshIdentityStateManager;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
        var options = new OptionsBuilder().addTitle("sshConfiguration");
        if (OsType.ofLocal() == OsType.WINDOWS) {
            options.addComp(prefs.getCustomOptions("x11WslInstance").buildComp());
        }

        var agentTest = new SshAgentTestComp(new SimpleObjectProperty<>(CustomAgentStrategy.builder().build()));
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
                    new OptionsBuilder().nameAndDescription("sshAgentSocket").addComp(choice, prefs.sshAgentSocket).addComp(agentTest));
        }
        return options.buildComp();
    }
}
