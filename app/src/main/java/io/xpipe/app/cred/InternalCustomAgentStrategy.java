package io.xpipe.app.cred;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.Validator;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("customAgent")
@Value
@Jacksonized
@Builder
public class InternalCustomAgentStrategy implements SshIdentityStrategy {

    boolean forwardAgent;
    String publicKey;

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        if (parent.isLocal()) {
            var agent = AppPrefs.get().sshAgentSocket().getValue();
            if (agent == null) {
                agent = AppPrefs.get().defaultSshAgentSocket().getValue();
            }
            SshIdentityStateManager.prepareLocalCustomAgent(
                    parent, agent);
        }
    }

    @Override
    public void buildCommand(CommandBuilder builder) {}

    private String getIdentityAgent(ShellControl sc) throws Exception {
        if (!sc.isLocal() || sc.getOsType() == OsType.WINDOWS) {
            return null;
        }

        if (AppPrefs.get() != null) {
            var agent = AppPrefs.get().sshAgentSocket().getValue();
            if (agent == null) {
                agent = AppPrefs.get().defaultSshAgentSocket().getValue();
            }
            if (agent != null) {
                return agent.resolveTildeHome(sc.view().userHome()).toString();
            }
        }

        return null;
    }

    @Override
    public List<KeyValue> configOptions(ShellControl sc) throws Exception {
        var file = SshIdentityStrategy.getPublicKeyPath(sc, publicKey);
        var l = new ArrayList<>(List.of(
                new KeyValue("IdentitiesOnly", file.isPresent() ? "yes" : "no"),
                new KeyValue("ForwardAgent", forwardAgent ? "yes" : "no"),
                new KeyValue("IdentityFile", file.isPresent() ? file.get().toString() : "none"),
                new KeyValue("PKCS11Provider", "none")));

        var agent = getIdentityAgent(sc);
        if (agent != null) {
            l.add(new KeyValue("IdentityAgent", "\"" + agent + "\""));
        }

        return l;
    }
}
