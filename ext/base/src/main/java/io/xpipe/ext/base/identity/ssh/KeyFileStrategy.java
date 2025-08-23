package io.xpipe.ext.base.identity.ssh;

import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.comp.base.ContextualFileReferenceSync;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.ContextualFileReference;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.SecretRetrievalStrategy;
import io.xpipe.app.util.SecretRetrievalStrategyHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;
import java.util.List;

@Value
@Jacksonized
@Builder
@JsonTypeName("file")
@AllArgsConstructor
public class KeyFileStrategy implements SshIdentityStrategy {

    @SuppressWarnings("unused")
    public static String getOptionsNameKey() {
        return "keyFile";
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<KeyFileStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var keyPath = new SimpleObjectProperty<>(
                p.getValue() != null && p.getValue().getFile() != null
                        ? p.getValue().getFile().toAbsoluteFilePath(null)
                        : null);
        p.addListener((observable, oldValue, newValue) -> {
            if (keyPath.get() != null
                    && newValue != null
                    && !ContextualFileReference.of(keyPath.get()).equals(newValue.getFile())) {
                return;
            }

            keyPath.setValue(
                    newValue != null && newValue.getFile() != null
                            ? newValue.getFile().toAbsoluteFilePath(null)
                            : null);
        });
        var keyPasswordProperty =
                new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getPassword() : null);

        var sync = new ContextualFileReferenceSync(
                Path.of("keys"), config.getPerUserKeyFileCheck(), path -> Path.of("keys")
                        .resolve(path.getFileName()));

        return new OptionsBuilder()
                .name("location")
                .description("locationDescription")
                .addComp(
                        new ContextualFileReferenceChoiceComp(
                                config.getProxy(), keyPath, config.isAllowKeyFileSync() ? sync : null, List.of()),
                        keyPath)
                .nonNull()
                .name("keyPassword")
                .description("sshConfigHost.identityPassphraseDescription")
                .sub(SecretRetrievalStrategyHelper.comp(keyPasswordProperty, true), keyPasswordProperty)
                .nonNull()
                .bind(
                        () -> {
                            return new KeyFileStrategy(
                                    ContextualFileReference.of(keyPath.get()), keyPasswordProperty.get());
                        },
                        p);
    }

    ContextualFileReference file;
    SecretRetrievalStrategy password;

    public void checkComplete() throws ValidationException {
        Validators.nonNull(file);
        Validators.nonNull(password);
    }

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        if (file == null) {
            return;
        }

        var s = file.toAbsoluteFilePath(parent);
        // The ~ is supported on all platforms, so manually replace it here for Windows
        if (s.startsWith("~")) {
            s = s.resolveTildeHome(parent.view().userHome());
        }
        var resolved = parent.getShellDialect()
                .evaluateExpression(parent, s.toString())
                .readStdoutOrThrow();
        if (!parent.getShellDialect().createFileExistsCommand(parent, resolved).executeAndCheck()) {
            var systemName = parent.getSourceStore()
                    .flatMap(shellStore -> DataStorage.get().getStoreEntryIfPresent(shellStore, false))
                    .map(e -> DataStorage.get().getStoreEntryDisplayName(e));
            var msg = "Identity file " + resolved + " does not exist"
                    + (systemName.isPresent() ? " on system " + systemName.get() : "");
            throw ErrorEventFactory.expected(new IllegalArgumentException(msg));
        }

        if (resolved.endsWith(".ppk")) {
            var ex = new IllegalArgumentException("Identity file " + resolved
                    + " is in non-standard PuTTY Private Key format (.ppk), which is not supported by OpenSSH. Please export/convert it to a "
                    + "standard format like .pem via PuTTY");
            ErrorEventFactory.preconfigure(
                    ErrorEventFactory.fromThrowable(ex).expected().link("https://www.puttygen.com/convert-pem-to-ppk"));
            throw ex;
        }

        if (resolved.endsWith(".pub")) {
            throw ErrorEventFactory.expected(new IllegalArgumentException("Identity file " + resolved
                    + " is marked to be a public key file, SSH authentication requires the private key"));
        }

        if (parent.getOsType() != OsType.WINDOWS) {
            // Try to preserve the same permission set
            parent.command(CommandBuilder.of()
                            .add("test", "-w")
                            .addFile(resolved)
                            .add("&&", "chmod", "600")
                            .addFile(resolved)
                            .add("||", "chmod", "400")
                            .addFile(resolved))
                    .executeAndCheck();
        }
    }

    @Override
    public void buildCommand(CommandBuilder builder) {}

    @Override
    public List<KeyValue> configOptions() {
        return List.of(
                new KeyValue("IdentitiesOnly", "yes"),
                new KeyValue("IdentityAgent", "none"),
                new KeyValue("IdentityFile", resolveFilePath().toString()),
                new KeyValue("PKCS11Provider", "none"));
    }

    @Override
    public SecretRetrievalStrategy getAskpassStrategy() {
        return password;
    }

    private FilePath resolveFilePath() {
        var s = file.toLocalAbsoluteFilePath();
        // The ~ is supported on all platforms, so manually replace it here for Windows
        if (s.startsWith("~")) {
            s = s.resolveTildeHome(FilePath.of(AppSystemInfo.ofCurrent().getUserHome()));
        }
        return s;
    }
}
