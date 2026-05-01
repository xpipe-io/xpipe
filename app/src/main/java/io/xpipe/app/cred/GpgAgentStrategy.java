package io.xpipe.app.cred;

import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Value
@Jacksonized
@Builder
@JsonTypeName("gpgAgent")
public class GpgAgentStrategy implements SshIdentityAgentStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<GpgAgentStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var publicKey =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);
        return new OptionsBuilder()
                .nameAndDescription("publicKey")
                .documentationLink(DocumentationLink.SSH_AGENT_PUBLIC_KEYS)
                .addComp(new SshAgentKeyListComp(config.getFileSystem(), p, publicKey, false), publicKey)
                .addComp(
                        new TextFieldComp(publicKey)
                                .apply(struc -> struc.setPromptText(
                                        "ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIBmhLUTJiP...== Your Comment")),
                        publicKey)
                .bind(
                        () -> {
                            return new GpgAgentStrategy(publicKey.get());
                        },
                        p);
    }

    private static Boolean supported;

    public static boolean isSupported() {
        if (supported != null) {
            return supported;
        }

        if (OsType.ofLocal() == OsType.WINDOWS) {
            var file = AppSystemInfo.ofWindows().getRoamingAppData().resolve("gnupg", "gpg-agent.conf");
            return (supported = Files.exists(file));
        } else {
            var file = AppSystemInfo.ofCurrent().getUserHome().resolve(".gnupg", "gpg-agent.conf");
            return (supported = Files.exists(file));
        }
    }

    String publicKey;

    @Override
    public void checkComplete() {

    }

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        parent.requireLicensedFeature(LicenseProvider.get().getFeature("gpgAgent"));
        if (parent.isLocal()) {
            SshIdentityStateManager.prepareLocalGpgAgent();
        }
    }

    @Override
    public FilePath determineAgentSocketLocation(ShellControl sc) throws Exception {
        if (sc.getOsType() == OsType.WINDOWS) {
            return null;
        }

        var r = sc.command("gpgconf --list-dirs agent-ssh-socket").readStdoutOrThrow();
        if (r.isEmpty()) {
            return null;
        }

        return FilePath.of(r);
    }

    @Override
    public void buildCommand(CommandBuilder builder) {}

    @Override
    public List<KeyValue> configOptions(ShellControl sc) throws Exception {
        var file = SshIdentityStrategy.getPublicKeyPath(sc, publicKey);
        var l = new ArrayList<>(List.of(
                KeyValue.raw("IdentitiesOnly", file.isPresent() ? "yes" : "no"),
                KeyValue.escape("IdentityFile", file.isPresent() ? file.get() : "none"),
                KeyValue.raw("PKCS11Provider", "none")));

        var agent = determineAgentSocketLocation(sc);
        if (agent != null) {
            l.add(KeyValue.escape("IdentityAgent", agent));
        }

        return l;
    }

    public PublicKeyStrategy getPublicKeyStrategy() {
        return PublicKeyStrategy.Fixed.of(publicKey);
    }
}
