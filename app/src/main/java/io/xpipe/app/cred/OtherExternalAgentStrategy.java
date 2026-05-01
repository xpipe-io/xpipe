package io.xpipe.app.cred;

import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@JsonTypeName("otherExternal")
@Value
@Jacksonized
@Builder
public class OtherExternalAgentStrategy implements SshIdentityAgentStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(
            Property<OtherExternalAgentStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var publicKey =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);
        return new OptionsBuilder()
                .nameAndDescription("publicKey")
                .documentationLink(DocumentationLink.SSH_AGENT_PUBLIC_KEYS)
                .addComp(new SshAgentKeyListComp(config.getFileSystem(), p, publicKey, false), publicKey)
                .bind(
                        () -> {
                            return new OtherExternalAgentStrategy(publicKey.get());
                        },
                        p);
    }

    String publicKey;

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        if (parent.isLocal()) {
            SshIdentityStateManager.prepareLocalExternalAgent(null);
        }
    }

    @Override
    public void checkComplete() {

    }

    @Override
    public FilePath determineAgentSocketLocation(ShellControl parent) {
        return null;
    }

    @Override
    public void buildCommand(CommandBuilder builder) {}

    @Override
    public List<KeyValue> configOptions(ShellControl sc) throws Exception {
        var file = SshIdentityStrategy.getPublicKeyPath(sc, publicKey);
        return List.of(
                KeyValue.raw("IdentitiesOnly", file.isPresent() ? "yes" : "no"),
                KeyValue.escape("IdentityFile", file.isPresent() ? file.get() : "none"),
                KeyValue.raw("PKCS11Provider", "none"));
    }

    public PublicKeyStrategy getPublicKeyStrategy() {
        return PublicKeyStrategy.Fixed.of(publicKey);
    }
}
