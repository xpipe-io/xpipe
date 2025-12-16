package io.xpipe.ext.base.identity.ssh;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.ClipboardHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.secret.SecretStrategyChoiceConfig;
import io.xpipe.app.storage.ContextualFileReference;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Files;
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
        var publicKey = new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getPublicKey() : null);

        var sync = ContextualFileReferenceSync.of(
                DataStorage.get().getDataDir().resolve("keys"),
                file -> file.getFileName().toString(),
                config.getPerUserKeyFileCheck());

        var passwordChoice = OptionsChoiceBuilder.builder()
                .allowNull(false)
                .property(keyPasswordProperty)
                .customConfiguration(
                        SecretStrategyChoiceConfig.builder().allowNone(true).build())
                .available(SecretRetrievalStrategy.getClasses())
                .build()
                .build();

        var publicKeyField = new TextFieldComp(publicKey).apply(struc -> {
            struc.get().promptTextProperty().bind(Bindings.createStringBinding(() -> {
                return "ssh-... ABCDEF.... (" + AppI18n.get("publicKeyGenerateNotice") + ")";
            }, AppI18n.activeLanguage()));
            struc.get().setEditable(false);
        });
        var generateButton = new ButtonComp(null, new LabelGraphic.IconGraphic("mdi2c-cog-refresh-outline"), () -> {
            ThreadHelper.runFailableAsync(() -> {
                Path path = keyPath.get().asLocalPath();
                if (!Files.exists(path)) {
                    return;
                }

                var pubKeyPath = Path.of(path + ".pub");
                if (Files.exists(pubKeyPath)) {
                    var contents = Files.readString(pubKeyPath).strip();
                    Platform.runLater(() -> {
                        publicKey.set(contents);
                    });
                }

                var contents = Files.readAllBytes(path);
                var generated = ProcessControlProvider.get().generatePublicSshKey(InPlaceSecretValue.of(contents), keyPasswordProperty.get());
                if (generated != null) {
                    Platform.runLater(() -> {
                        publicKey.set(generated);
                    });
                }
            });
        }).tooltipKey("generatePublicKey").disable(keyPath.isNull().or(publicKey.isNotNull()).or(keyPasswordProperty.isNull()));
        var copyButton = new ButtonComp(null, new FontIcon("mdi2c-clipboard-multiple-outline"), () -> {
            ClipboardHelper.copyText(publicKey.get());
        })
                .disable(publicKey.isNull())
                .tooltipKey("copyPublicKey");

        var publicKeyBox = new InputGroupComp(List.of(publicKeyField, copyButton, generateButton));
        publicKeyBox.setMainReference(publicKeyField);

        return new OptionsBuilder()
                .name("location")
                .description("locationDescription")
                .addComp(
                        new ContextualFileReferenceChoiceComp(
                                new ReadOnlyObjectWrapper<>(
                                        DataStorage.get().local().ref()),
                                keyPath,
                                config.isAllowKeyFileSync() ? sync : null,
                                List.of(),
                                e -> e.equals(DataStorage.get().local())),
                        keyPath)
                .nonNull()
                .name("keyPassword")
                .description("sshConfigHost.identityPassphraseDescription")
                .sub(passwordChoice, keyPasswordProperty)
                .nonNull()
                .nameAndDescription("inPlacePublicKey")
                .addComp(
                        publicKeyBox,
                        publicKey)
                .bind(
                        () -> {
                            return new KeyFileStrategy(
                                    ContextualFileReference.of(keyPath.get()), keyPasswordProperty.get(), publicKey.get());
                        },
                        p);
    }

    ContextualFileReference file;
    SecretRetrievalStrategy password;
    String publicKey;

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
                new KeyValue("IdentityFile", "\"" + resolveFilePath().toString() + "\""),
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
