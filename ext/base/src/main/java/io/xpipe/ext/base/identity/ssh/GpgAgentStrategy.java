package io.xpipe.ext.base.identity.ssh;

import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Files;
import java.util.List;

@Value
@Jacksonized
@Builder
@JsonTypeName("gpgAgent")
public class GpgAgentStrategy implements SshIdentityStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<GpgAgentStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var forward =
                new SimpleBooleanProperty(p.getValue() != null && p.getValue().isForwardAgent());
        var publicKey =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);
        return new OptionsBuilder()
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
                            return new GpgAgentStrategy(forward.get(), publicKey.get());
                        },
                        p);
    }

    private static Boolean supported;

    public static boolean isSupported() {
        if (supported != null) {
            return supported;
        }

        try {
            var found = LocalShell.getShell()
                    .view()
                    .findProgram("gpg-connect-agent")
                    .isPresent();
            if (!found) {
                return (supported = false);
            }
        } catch (Exception ex) {
            return (supported = false);
        }

        if (OsType.getLocal() == OsType.WINDOWS) {
            var file = AppSystemInfo.ofWindows().getRoamingAppData().resolve("gnupg", "gpg-agent.conf");
            return (supported = Files.exists(file));
        } else {
            var file = AppSystemInfo.ofCurrent().getUserHome().resolve(".gnupg", "gpg-agent.conf");
            return (supported = Files.exists(file));
        }
    }

    boolean forwardAgent;
    String publicKey;

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        parent.requireLicensedFeature(LicenseProvider.get().getFeature("gpgAgent"));
        if (parent.isLocal()) {
            SshIdentityStateManager.prepareLocalGpgAgent();
        }
    }

    @Override
    public void buildCommand(CommandBuilder builder) {
        builder.environment("SSH_AUTH_SOCK", sc -> {
            if (sc.getOsType() == OsType.WINDOWS) {
                return null;
            }

            var r = sc.executeSimpleStringCommand("gpgconf --list-dirs agent-ssh-socket");
            return r;
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
