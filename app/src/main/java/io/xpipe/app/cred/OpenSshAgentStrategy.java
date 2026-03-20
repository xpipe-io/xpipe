package io.xpipe.app.cred;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("sshAgent")
@Value
@Jacksonized
@Builder
public class OpenSshAgentStrategy implements SshIdentityAgentStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(
            Property<OpenSshAgentStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var socket = AppPrefs.get().defaultSshAgentSocket().getValue();
        var publicKey =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);
        return new OptionsBuilder()
                .nameAndDescription("agentSocket")
                .addStaticString(socket != null ? socket : AppI18n.get("agentSocketNotFound"))
                .hide(OsType.ofLocal() == OsType.WINDOWS)
                .nameAndDescription("publicKey")
                .addComp(new SshAgentKeyListComp(config.getFileSystem(), p, publicKey, false), publicKey)
                .bind(
                        () -> {
                            return new OpenSshAgentStrategy(publicKey.get());
                        },
                        p);
    }

    String publicKey;

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        if (parent.isLocal()) {
            SshIdentityStateManager.prepareLocalOpenSshAgent(
                    parent, AppPrefs.get().defaultSshAgentSocket().getValue());
        }
    }

    @Override
    public FilePath determinetAgentSocketLocation(ShellControl sc) throws Exception {
        if (sc.getOsType() == OsType.WINDOWS) {
            return null;
        }

        if (AppPrefs.get() != null) {
            var socket = AppPrefs.get().defaultSshAgentSocket().getValue();
            if (socket != null) {
                return socket.resolveTildeHome(sc.view().userHome());
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
