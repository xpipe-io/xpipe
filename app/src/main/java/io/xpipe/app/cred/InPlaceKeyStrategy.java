package io.xpipe.app.cred;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.comp.base.TextAreaComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.platform.*;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.secret.SecretStrategyChoiceConfig;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.LocalFileTracker;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.*;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Value
@Jacksonized
@Builder
@JsonTypeName("inPlaceKey")
@AllArgsConstructor
public class InPlaceKeyStrategy implements SshIdentityStrategy {

    private static final Set<String> KEYS = new HashSet<>();

    public static boolean isInPlaceKey(String keyName) {
        return KEYS.contains(keyName);
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<InPlaceKeyStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var options = new OptionsBuilder();

        var key = options.map(p, InPlaceKeyStrategy::getKey, SecretValue::getSecretValue);
        var publicKey = options.map(p, InPlaceKeyStrategy::getPublicKey);
        var keyPasswordProperty = options.map(p, InPlaceKeyStrategy::getPassword);

        var passwordChoice = OptionsChoiceBuilder.builder()
                .allowNull(false)
                .property(keyPasswordProperty)
                .customConfiguration(SecretStrategyChoiceConfig.builder()
                        .allowNone(true)
                        .passwordKey("passphrase")
                        .build())
                .available(SecretRetrievalStrategy.getClasses())
                .build()
                .build();
        var publicKeyField = new TextFieldComp(publicKey).apply(struc -> {
            struc.promptTextProperty()
                    .bind(Bindings.createStringBinding(
                            () -> {
                                return "ssh-... ABCDEF.... (" + AppI18n.get("publicKeyGenerateNotice") + ")";
                            },
                            AppI18n.activeLanguage()));
            struc.setEditable(false);
        });
        var generatedKeyBase = new SimpleObjectProperty<>(key.get());
        var generateButtonDisabled = Bindings.createBooleanBinding(
                () -> {
                    return key.get() == null
                            || (publicKey.get() != null && key.get().equals(generatedKeyBase.get()));
                },
                key,
                publicKey);
        var generateButton = new ButtonComp(null, new LabelGraphic.IconGraphic("mdi2c-cog-refresh-outline"), () -> {
                    ThreadHelper.runAsync(() -> {
                        var generated = ProcessControlProvider.get()
                                .generatePublicSshKey(InPlaceSecretValue.of(key.get()), keyPasswordProperty.get());
                        if (generated != null) {
                            publicKey.set(generated);
                            generatedKeyBase.set(key.getValue());
                        }
                    });
                })
                .describe(d -> d.nameKey("generatePublicKey"))
                .disable(generateButtonDisabled);
        var copyButton = new ButtonComp(null, new FontIcon("mdi2c-clipboard-multiple-outline"), () -> {
                    ClipboardHelper.copyText(publicKey.get());
                })
                .disable(publicKey.isNull())
                .describe(d -> d.nameKey("copyPublicKey"));

        var publicKeyBox = new InputGroupComp(List.of(publicKeyField, copyButton, generateButton));
        publicKeyBox.setMainReference(publicKeyField);

        return options.nameAndDescription("inPlaceKeyText")
                .addComp(
                        new TextAreaComp(key).applyStructure(struc -> {
                            struc.getTextArea().setPromptText("""
                                                      -----BEGIN ... PRIVATE KEY-----


                                                      -----END   ... PRIVATE KEY-----
                                                      """);
                            struc.getTextArea().setPrefRowCount(4);
                        }),
                        key)
                .nonNull()
                .nameAndDescription("keyPassphrase")
                .sub(passwordChoice, keyPasswordProperty)
                .nonNull()
                .nameAndDescription("inPlacePublicKey")
                .documentationLink(DocumentationLink.SSH_PUBLIC_KEY)
                .addComp(publicKeyBox, publicKey)
                .bind(
                        () -> {
                            return new InPlaceKeyStrategy(
                                    key.getValue() != null ? InPlaceSecretValue.of(key.getValue()) : null,
                                    publicKey.get(),
                                    keyPasswordProperty.getValue());
                        },
                        p);
    }

    SecretValue key;
    String publicKey;
    SecretRetrievalStrategy password;

    public void checkComplete() throws ValidationException {
        Validators.nonNull(key);
        Validators.nonNull(password);
    }

    @Override
    public synchronized void prepareParent(ShellControl parent) throws Exception {
        if (key == null) {
            return;
        }

        var file = getTargetFilePath(parent);
        // Don't spam log each time it checks for file existence
        if (parent.getShellDialect()
                .createFileExistsCommand(parent, file.toString())
                .sensitive()
                .executeAndCheck()) {
            return;
        }

        parent.view().touch(file);
        if (parent.getOsType() != OsType.WINDOWS) {
            parent.command(CommandBuilder.of().add("chmod", "600").addFile(file))
                    .execute();
        }
        // Make sure that the line endings are in LF
        // to support older SSH clients that break with CRLF
        var bytes = (key.getSecretValue().lines().collect(Collectors.joining("\n")) + "\n")
                .getBytes(StandardCharsets.UTF_8);
        parent.view().writeRawFile(file, bytes);
        if (parent.getOsType() != OsType.WINDOWS) {
            parent.command(CommandBuilder.of().add("chmod", "400").addFile(file))
                    .execute();
        }

        if (parent.isLocal()) {
            LocalFileTracker.deleteOnExit(file.asLocalPath());
        }
    }

    @Override
    public void buildCommand(CommandBuilder builder) {}

    @Override
    public List<KeyValue> configOptions(ShellControl sc) {
        return List.of(
                KeyValue.raw("IdentitiesOnly", "yes"),
                KeyValue.raw("IdentityAgent", "none"),
                KeyValue.escape("IdentityFile", getTargetFilePath(sc)),
                KeyValue.raw("PKCS11Provider", "none"));
    }

    @Override
    public SecretRetrievalStrategy getAskpassStrategy() {
        return password;
    }

    private FilePath getTargetFilePath(ShellControl sc) {
        var hash = Math.abs(Objects.hash(this, AppSystemInfo.ofCurrent().getUser()));
        var temp = sc.getSystemTemporaryDirectory().join("xpipe-" + hash + ".key");
        KEYS.add(temp.getFileName());
        return temp;
    }

    public PublicKeyStrategy getPublicKeyStrategy() {
        return PublicKeyStrategy.Fixed.of(publicKey);
    }
}
