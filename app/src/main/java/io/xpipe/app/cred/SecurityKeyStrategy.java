package io.xpipe.app.cred;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
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
        var filePath = new SimpleObjectProperty<String>();
        securityKey.subscribe(impl -> {
            if (impl == null) {
                filePath.set(null);
                return;
            }

            ThreadHelper.runFailableAsync(() -> {
                var fs = config.getFileSystem() != null && config.getFileSystem().getValue() != null
                        ? config.getFileSystem().getValue().getStore()
                        : (ShellStore) DataStorage.get().local().getStore().asNeeded();
                var path = impl.determineLibraryPath(fs.getOrStartSession());
                filePath.set(path != null ? path.toString() : null);
            });
        });
        if (config.getFileSystem() != null) {
            config.getFileSystem().subscribe(fs -> {
                if (fs == null) {
                    filePath.set(null);
                    return;
                }

                ThreadHelper.runFailableAsync(() -> {
                    var impl = securityKey.get();
                    if (impl != null) {
                        filePath.set(impl.determineLibraryPath(fs.getStore().getOrStartSession())
                                .toString());
                    } else {
                        filePath.set(null);
                    }
                });
            });
        }

        var choice = OptionsChoiceBuilder.builder()
                .property(securityKey)
                .available(SecurityKeyImpl.getAvailable())
                .customConfiguration(config)
                .build()
                .build();

        var showLibraryPath = Bindings.createBooleanBinding(() -> {
            if (filePath.get() == null) {
                return false;
            }

            if (securityKey.get() == null) {
                return false;
            }

            return securityKey.get().showLibraryPath();
        }, filePath, securityKey);

        return new OptionsBuilder()
                .nameAndDescription("pkcs11Impl")
                .sub(choice, securityKey)
                .nonNull()
                .nameAndDescription("pkcs11Library")
                .addStaticString(filePath)
                .hide(Bindings.not(showLibraryPath))
                .nameAndDescription("publicKey")
                .documentationLink(DocumentationLink.SSH_AGENT_PUBLIC_KEYS)
                .addComp(new SshAgentKeyListComp(config.getFileSystem(), p, publicKey, false, true), publicKey)
                .bind(
                        () -> {
                            return SecurityKeyStrategy.builder()
                                    .securityKey(securityKey.get())
                                    .publicKey(publicKey.get())
                                    .build();
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
        var file = securityKey.determineLibraryPath(parent);
        if (!parent.view().fileExists(file)) {
            var ex = new IOException("PKCS11 library at " + file + " not found");
            var event = ErrorEventFactory.fromThrowable(ex).expected();
            if (securityKey.getLink() != null) {
                event.link(securityKey.getLink());
            }
            ErrorEventFactory.preconfigure(event);
            throw ex;
        }
    }

    @Override
    public CommandBuilder createListCommand() {
        var cmd = CommandBuilder.of()
                .add("ssh-keygen", "-D")
                .addFile(sc -> securityKey.determineLibraryPath(sc).toUnix())
                .add("-e")
                .fixedEnvironment(
                        "SSH_ASKPASS",
                        AppInstallation.ofCurrent().getCliExecutablePath().toString())
                .fixedEnvironment("SSH_ASKPASS_REQUIRE", "force");
        ProcessControlProvider.get().addAskpassEnvironment(cmd, "[ssh-keygen]", null, null);
        return cmd;
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
        var key = SshIdentityStrategy.getPublicKeyPath(sc, publicKey);
        return List.of(
                KeyValue.escape("PKCS11Provider", file.toString()),
                KeyValue.raw("IdentitiesOnly", key.isPresent() ? "yes" : "no"),
                KeyValue.escape("IdentityFile", key.isPresent() ? key.get().toString() : "none"),
                KeyValue.raw("IdentityAgent", "none"));
    }

    @Override
    public PublicKeyStrategy getPublicKeyStrategy() {
        return PublicKeyStrategy.Fixed.of(publicKey);
    }
}
