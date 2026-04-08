package io.xpipe.app.cred;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.util.List;

@Value
@Jacksonized
@Builder
@JsonTypeName("hardwareSecurityKey")
@AllArgsConstructor
public class SecurityKeyStrategy implements SshIdentityKeyListStrategy {


    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(
            Property<SecurityKeyStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var publicKey = new SimpleStringProperty(p.getValue().getPublicKey());
        var securityKey = new SimpleObjectProperty<>(p.getValue().getSecurityKey());

        var choice = OptionsChoiceBuilder.builder().property(securityKey).available(SecurityKeyImpl.getAvailable()).customConfiguration(config).build().build();

        return new OptionsBuilder()
                .nameAndDescription("pkcs11Library")
                .sub(choice, securityKey)
                .nonNull()
                .nameAndDescription("publicKey")
                .documentationLink(DocumentationLink.SSH_AGENT_PUBLIC_KEYS)
                .addComp(new SshAgentKeyListComp(config.getFileSystem(), p, publicKey, false), publicKey)
                .bind(
                        () -> {
                            return SecurityKeyStrategy.builder().securityKey(securityKey.get()).publicKey(publicKey.get()).build();
                        },
                        p);
    }

    SecurityKeyImpl securityKey;
    String publicKey;

    @Override
    public void checkComplete() throws ValidationException {
        Validators.nonNull(securityKey);
        securityKey.checkComplete();
    }

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        parent.requireLicensedFeature(LicenseProvider.get().getFeature("pkcs11Identity"));

        var file = securityKey.determineLibraryPath(parent);
        if (!parent.view().fileExists(file)) {
            throw ErrorEventFactory.expected(new IOException("PKCS11 library at " + file + " not found"));
        }
    }

    @Override
    public CommandBuilder createListCommand() {
        return CommandBuilder.of().add("ssh-keygen", "-D").addFile(sc -> securityKey.determineLibraryPath(sc).toUnix()).add("-e");
    }

    @Override
    public void buildCommand(CommandBuilder builder) {
        builder.setup(sc -> {
            var dir = securityKey.determineLibraryPath(sc).getParent();
            if (sc.getOsType() == OsType.WINDOWS) {
                builder.addToPath(dir, true);
            } else {
                builder.addToEnvironmentPath("LD_LIBRARY_PATH", dir, true);
            }
        });
    }

    @Override
    public List<KeyValue> configOptions(ShellControl sc) throws Exception {
        var file = securityKey.determineLibraryPath(sc);
        return List.of(
                new KeyValue("IdentitiesOnly", "no"),
                new KeyValue("PKCS11Provider", "\"" + file.toString() + "\""),
                new KeyValue("IdentityFile", "none"),
                new KeyValue("IdentityAgent", "none"));
    }

    public PublicKeyStrategy getPublicKeyStrategy() {
        return null;
    }
}
