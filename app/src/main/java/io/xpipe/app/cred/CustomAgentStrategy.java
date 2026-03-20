package io.xpipe.app.cred;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.Validator;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;

import com.fasterxml.jackson.annotation.JsonTypeName;
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
public class CustomAgentStrategy implements SshIdentityAgentStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(
            Property<CustomAgentStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var publicKey =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);

        var socketBinding = Bindings.createObjectBinding(
                () -> {
                    var agent = AppPrefs.get().sshAgentSocket().getValue();
                    if (agent == null) {
                        agent = AppPrefs.get().defaultSshAgentSocket().getValue();
                    }
                    return agent != null ? agent.toString() : AppI18n.get("agentSocketNotConfigured");
                },
                AppPrefs.get().defaultSshAgentSocket(),
                AppPrefs.get().sshAgentSocket());
        var socketProp = new SimpleStringProperty();
        socketProp.bind(socketBinding);
        var socketDisplay = new HorizontalComp(List.of(
                        new TextFieldComp(socketProp)
                                .apply(struc -> struc.setEditable(false))
                                .hgrow(),
                        new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
                                    AppPrefs.get().selectCategory("ssh");
                                })
                                .padding(new Insets(7))))
                .spacing(9);

        return new OptionsBuilder()
                .nameAndDescription("agentSocket")
                .addComp(socketDisplay)
                .check(val -> Validator.create(
                        val,
                        AppI18n.observable("agentSocketNotConfigured"),
                        Bindings.createObjectBinding(
                                () -> {
                                    var agent = AppPrefs.get().sshAgentSocket().getValue();
                                    if (agent == null) {
                                        agent = AppPrefs.get()
                                                .defaultSshAgentSocket()
                                                .getValue();
                                    }
                                    return agent;
                                },
                                AppPrefs.get().sshAgentSocket(),
                                AppPrefs.get().defaultSshAgentSocket()),
                        i -> {
                            return i != null;
                        }))
                .nameAndDescription("publicKey")
                .addComp(new SshAgentKeyListComp(config.getFileSystem(), p, publicKey, false), publicKey)
                .bind(
                        () -> {
                            return new CustomAgentStrategy(publicKey.get());
                        },
                        p);
    }

    String publicKey;

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        if (parent.isLocal()) {
            var agent = AppPrefs.get().sshAgentSocket().getValue();
            if (agent == null) {
                agent = AppPrefs.get().defaultSshAgentSocket().getValue();
            }
            SshIdentityStateManager.prepareLocalCustomAgent(parent, agent);
        }
    }

    @Override
    public FilePath determinetAgentSocketLocation(ShellControl sc) throws Exception {
        if (!sc.isLocal() || sc.getOsType() == OsType.WINDOWS) {
            return null;
        }

        if (AppPrefs.get() != null) {
            var agent = AppPrefs.get().sshAgentSocket().getValue();
            if (agent == null) {
                agent = AppPrefs.get().defaultSshAgentSocket().getValue();
            }
            if (agent != null) {
                return agent.resolveTildeHome(sc.view().userHome());
            }
        }

        return null;
    }

    @Override
    public void buildCommand(CommandBuilder builder) {}

    @Override
    public List<KeyValue> configOptions(ShellControl sc) throws Exception {
        var file = SshIdentityStrategy.getPublicKeyPath(sc, publicKey);
        var l = new ArrayList<>(List.of(
                new KeyValue("IdentitiesOnly", file.isPresent() ? "yes" : "no"),
                new KeyValue("IdentityFile", file.isPresent() ? file.get().toString() : "none"),
                new KeyValue("PKCS11Provider", "none")));

        var agent = determinetAgentSocketLocation(sc);
        if (agent != null) {
            l.add(new KeyValue("IdentityAgent", "\"" + agent + "\""));
        }

        return l;
    }

    public PublicKeyStrategy getPublicKeyStrategy() {
        return PublicKeyStrategy.Fixed.of(publicKey);
    }
}
