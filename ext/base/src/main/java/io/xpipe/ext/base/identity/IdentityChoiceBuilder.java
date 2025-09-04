package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.secret.EncryptedValue;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.secret.SecretStrategyChoiceConfig;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.*;
import io.xpipe.ext.base.identity.ssh.SshIdentityStrategy;
import io.xpipe.ext.base.identity.ssh.SshIdentityStrategyChoiceConfig;

import javafx.beans.property.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class IdentityChoiceBuilder {

    Property<DataStoreEntryRef<ShellStore>> host;
    ObjectProperty<IdentityValue> identity;
    boolean allowCustomUserInput;
    boolean requireUserInput;
    boolean requirePassword;
    boolean keyInput;
    boolean allowAgentForward;
    String userChoiceTranslationKey;
    String passwordChoiceTranslationKey;

    public static OptionsBuilder ssh(
            Property<DataStoreEntryRef<ShellStore>> host, ObjectProperty<IdentityValue> identity, boolean requireUser) {
        var i = new IdentityChoiceBuilder(
                host, identity, true, requireUser, true, true, true, "identityChoice", "passwordAuthentication");
        return i.build();
    }

    public static OptionsBuilder container(ObjectProperty<IdentityValue> identity) {
        var i = new IdentityChoiceBuilder(
                null, identity, true, false, false, false, false, "customUsername", "customUsernamePassword");
        return i.build();
    }

    public OptionsBuilder build() {
        var existing = identity.getValue();
        var user = new SimpleStringProperty(
                existing instanceof IdentityValue.InPlace inPlace && inPlace.unwrap() != null
                        ? inPlace.unwrap().getUsername().get()
                        : null);
        var pass = new SimpleObjectProperty<>(
                existing instanceof IdentityValue.InPlace inPlace && inPlace.unwrap() != null
                        ? inPlace.unwrap().getPassword()
                        : null);
        var identityStrategy = new SimpleObjectProperty<>(
                existing instanceof IdentityValue.InPlace inPlace && inPlace.unwrap() != null
                        ? inPlace.unwrap().getSshIdentity()
                        : null);
        var ref = new SimpleObjectProperty<>(existing instanceof IdentityValue.Ref r ? r.getRef() : null);
        var inPlaceSelected = ref.isNull();
        var refSelected = ref.isNotNull();

        var passwordChoice = OptionsChoiceBuilder.builder()
                .allowNull(false)
                .property(pass)
                .customConfiguration(
                        SecretStrategyChoiceConfig.builder().allowNone(true).build())
                .available(SecretRetrievalStrategy.getSubclasses())
                .build()
                .build();

        var options = new OptionsBuilder()
                .nameAndDescription(userChoiceTranslationKey)
                .addComp(new IdentitySelectComp(ref, user, pass, identityStrategy, allowCustomUserInput), user)
                .nonNullIf(inPlaceSelected.and(new SimpleBooleanProperty(requireUserInput)))
                .nameAndDescription(passwordChoiceTranslationKey)
                .sub(passwordChoice, pass)
                .nonNullIf(inPlaceSelected.and(new SimpleBooleanProperty(requirePassword)))
                .hide(refSelected)
                .addProperty(ref);

        var sshIdentityChoiceConfig = SshIdentityStrategyChoiceConfig.builder()
                .allowAgentForward(allowAgentForward)
                .proxy(
                        host != null
                                ? host
                                : new ReadOnlyObjectWrapper<>(
                                        DataStorage.get().local().ref()))
                .allowKeyFileSync(true)
                .perUserKeyFileCheck(path -> false)
                .build();

        if (keyInput) {
            options.name("keyAuthentication")
                    .description("keyAuthenticationDescription")
                    .longDescription(DocumentationLink.SSH_KEYS)
                    .sub(
                            OptionsChoiceBuilder.builder()
                                    .allowNull(false)
                                    .property(identityStrategy)
                                    .customConfiguration(sshIdentityChoiceConfig)
                                    .available(SshIdentityStrategy.getSubclasses())
                                    .build()
                                    .build(),
                            identityStrategy)
                    .nonNullIf(inPlaceSelected)
                    .disable(refSelected)
                    .hide(refSelected);
        }
        options.bind(
                () -> {
                    if (ref.get() != null) {
                        return IdentityValue.Ref.builder().ref(ref.get()).build();
                    } else {
                        var u = user.get();
                        var p = EncryptedValue.CurrentKey.of(pass.get());
                        EncryptedValue<SshIdentityStrategy> i =
                                keyInput ? EncryptedValue.CurrentKey.of(identityStrategy.get()) : null;
                        if (u == null && p == null && i == null) {
                            return null;
                        } else {
                            return IdentityValue.InPlace.builder()
                                    .identityStore(LocalIdentityStore.builder()
                                            .username(u)
                                            .password(p)
                                            .sshIdentity(i)
                                            .build())
                                    .build();
                        }
                    }
                },
                identity);
        return options;
    }
}
