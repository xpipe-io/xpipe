package io.xpipe.ext.base.identity.ssh;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.comp.base.TextAreaComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.platform.ClipboardHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.secret.SecretStrategyChoiceConfig;
import io.xpipe.app.util.LocalFileTracker;
import io.xpipe.app.util.Validators;
import io.xpipe.core.*;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Jacksonized
@Builder
@JsonTypeName("inPlaceKey")
@AllArgsConstructor
public class InPlaceKeyStrategy implements SshIdentityStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<InPlaceKeyStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var options = new OptionsBuilder();

        var key = options.map(p, InPlaceKeyStrategy::getKey, SecretValue::getSecretValue);
        var publicKey = options.map(p, InPlaceKeyStrategy::getPublicKey);
        var keyPasswordProperty = options.map(p, InPlaceKeyStrategy::getPassword);

        var passwordChoice = OptionsChoiceBuilder.builder()
                .allowNull(false)
                .property(keyPasswordProperty)
                .customConfiguration(
                        SecretStrategyChoiceConfig.builder().allowNone(true).build())
                .available(SecretRetrievalStrategy.getSubclasses())
                .build()
                .build();
        var publicKeyField = new TextFieldComp(publicKey).apply(struc -> {
            struc.get().promptTextProperty().bind(Bindings.createStringBinding(() -> {
                return "ssh-... ABCDEF.... (" + AppI18n.get("publicKeyGenerateNotice") + ")";
            }, AppI18n.activeLanguage()));
            struc.get().setEditable(false);
        });
        var generateButton = new ButtonComp(null, new LabelGraphic.IconGraphic("mdi2c-cog-refresh-outline"), () -> {
            var generated = ProcessControlProvider.get().generatePublicSshKey(InPlaceSecretValue.of(key.get()), keyPasswordProperty.get());
            if (generated != null) {
                publicKey.set(generated);
            }
        }).tooltipKey("generatePublicKey").disable(key.isNull().or(publicKey.isNotNull()).or(keyPasswordProperty.isNull()));
        var copyButton = new ButtonComp(null, new FontIcon("mdi2c-clipboard-multiple-outline"), () -> {
            ClipboardHelper.copyText(publicKey.get());
        })
                .disable(publicKey.isNull())
                .tooltipKey("copyPublicKey");

        var publicKeyBox = new InputGroupComp(List.of(publicKeyField, copyButton, generateButton));
        publicKeyBox.setMainReference(publicKeyField);

        return options
                .nameAndDescription("inPlaceKeyText")
                .addComp(
                        new TextAreaComp(key).apply(struc -> {
                            struc.getTextArea()
                                    .setPromptText(
                                            """
                                                      -----BEGIN ... PRIVATE KEY-----


                                                      -----END   ... PRIVATE KEY-----
                                                      """);
                            struc.getTextArea().setPrefRowCount(4);
                        }),
                        key)
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

        var file = getTargetFilePath();
        if (parent.view().fileExists(file)) {
            return;
        }

        parent.view().touch(file);
        if (parent.getOsType() != OsType.WINDOWS) {
            parent.command(CommandBuilder.of().add("chmod", "600").addFile(file))
                    .execute();
        }
        // Make sure that the line endings are in LF
        // to support older SSH clients that break with CRLF
        var bytes = (key.getSecretValue().lines().collect(Collectors.joining("\n")) + "\n").getBytes(StandardCharsets.UTF_8);
        parent.view().writeRawFile(file, bytes);
        if (parent.getOsType() != OsType.WINDOWS) {
            parent.command(CommandBuilder.of().add("chmod", "400").addFile(file))
                    .execute();
        }

        LocalFileTracker.deleteOnExit(file.asLocalPath());
    }

    @Override
    public void buildCommand(CommandBuilder builder) {}

    @Override
    public List<KeyValue> configOptions() {
        return List.of(
                new KeyValue("IdentitiesOnly", "yes"),
                new KeyValue("IdentityAgent", "none"),
                new KeyValue("IdentityFile", "\"" + getTargetFilePath() + "\""),
                new KeyValue("PKCS11Provider", "none"));
    }

    @Override
    public SecretRetrievalStrategy getAskpassStrategy() {
        return password;
    }

    private FilePath getTargetFilePath() {
        var temp = AppSystemInfo.ofCurrent().getTemp().resolve("xpipe-" + Math.abs(hashCode()) + ".key");
        return FilePath.of(temp);
    }
}
