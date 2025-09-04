package io.xpipe.ext.base.identity.ssh;

import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@JsonTypeName("sshAgent")
@Value
@Jacksonized
@Builder
public class OpenSshAgentStrategy implements SshIdentityStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(
            Property<OpenSshAgentStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var socket = AppPrefs.get().defaultSshAgentSocket().getValue();
        var forward =
                new SimpleBooleanProperty(p.getValue() != null && p.getValue().isForwardAgent());
        var publicKey =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);
        return new OptionsBuilder()
                .nameAndDescription("agentSocket")
                .addStaticString(socket != null ? socket : AppI18n.get("agentSocketNotFound"))
                .hide(OsType.getLocal() == OsType.WINDOWS)
                .nameAndDescription("forwardAgent")
                .addToggle(forward)
                .nonNull()
                .hide(!config.isAllowAgentForward())
                .nameAndDescription("publicKey")
                .addComp(
                        new TextFieldComp(publicKey).apply(struc -> struc.get()
                                .setPromptText("ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIBmhLUTJiP...== Your Comment")),
                        publicKey)
                .bind(
                        () -> {
                            return new OpenSshAgentStrategy(forward.get(), publicKey.get());
                        },
                        p);
    }

    boolean forwardAgent;
    String publicKey;

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        if (parent.isLocal()) {
            SshIdentityStateManager.prepareLocalOpenSshAgent(
                    parent, AppPrefs.get().defaultSshAgentSocket().getValue());
        }
    }

    @Override
    public void buildCommand(CommandBuilder builder) {
        builder.environment("SSH_AUTH_SOCK", sc -> {
            if (sc.getOsType() == OsType.WINDOWS) {
                return null;
            }

            if (AppPrefs.get() != null) {
                var socket = AppPrefs.get().defaultSshAgentSocket().getValue();
                if (socket != null) {
                    return socket.resolveTildeHome(sc.view().userHome()).toString();
                }
            }

            return null;
        });
    }

    @Override
    public List<KeyValue> configOptions() {
        var file = SshIdentityStrategy.getPublicKeyPath(publicKey);
        return List.of(
                new KeyValue("IdentitiesOnly", file.isPresent() ? "yes" : "no"),
                new KeyValue("ForwardAgent", forwardAgent ? "yes" : "no"),
                new KeyValue("IdentityFile", file.isPresent() ? file.get().toString() : "none"),
                new KeyValue("PKCS11Provider", "none"));
    }
}
