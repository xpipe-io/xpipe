package io.xpipe.ext.base.identity.ssh;

import io.xpipe.app.comp.base.TextAreaComp;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.secret.SecretStrategyChoiceConfig;
import io.xpipe.app.util.LocalFileTracker;
import io.xpipe.app.util.Validators;
import io.xpipe.core.*;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Jacksonized
@Builder
@JsonTypeName("inPlaceKey")
@AllArgsConstructor
public class InPlaceKeyStrategy implements SshIdentityStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<InPlaceKeyStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var key = new SimpleStringProperty(
                p.getValue().getKey() != null ? p.getValue().getKey().getSecretValue() : null);
        var keyPasswordProperty =
                new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getPassword() : null);

        var passwordChoice = OptionsChoiceBuilder.builder()
                .allowNull(false)
                .property(keyPasswordProperty)
                .customConfiguration(
                        SecretStrategyChoiceConfig.builder().allowNone(true).build())
                .available(SecretRetrievalStrategy.getSubclasses())
                .build()
                .build();

        return new OptionsBuilder()
                .nameAndDescription("inPlaceKeyText")
                .addComp(
                        new TextAreaComp(key).apply(struc -> {
                            struc.getTextArea()
                                    .setPromptText(
                                            """
                                                      -----BEGIN ... PRIVATE KEY-----

                                                      -----END ... PRIVATE KEY-----
                                                      """);
                        }),
                        key)
                .nonNull()
                .name("keyPassword")
                .description("sshConfigHost.identityPassphraseDescription")
                .sub(passwordChoice, keyPasswordProperty)
                .nonNull()
                .bind(
                        () -> {
                            return new InPlaceKeyStrategy(
                                    key.get() != null ? InPlaceSecretValue.of(key.get()) : null,
                                    keyPasswordProperty.get());
                        },
                        p);
    }

    SecretValue key;
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
            parent.command(CommandBuilder.of().add("chmod", "600").addFile(file));
        }
        parent.view().writeTextFile(file, key.getSecretValue());
        if (parent.getOsType() != OsType.WINDOWS) {
            parent.command(CommandBuilder.of().add("chmod", "400").addFile(file));
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
