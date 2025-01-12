package io.xpipe.ext.base.identity;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.EncryptedValue;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.SecretRetrievalStrategyHelper;

import io.xpipe.app.util.Validator;
import javafx.beans.property.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class IdentityChoice {

    public static OptionsBuilder ssh(
            Property<DataStoreEntryRef<ShellStore>> gateway,
            ObjectProperty<IdentityValue> identity,
            boolean requireUser) {
        var i = new IdentityChoice(
                gateway, identity, requireUser, requireUser, true, true, "identityChoice", "passwordAuthentication");
        return i.build();
    }

    public static OptionsBuilder container(ObjectProperty<IdentityValue> identity) {
        var i = new IdentityChoice(
                null, identity, true, false, false, false, "customUsername", "customUsernamePassword");
        return i.build();
    }

    Property<DataStoreEntryRef<ShellStore>> gateway;
    ObjectProperty<IdentityValue> identity;
    boolean allowCustomUserInput;
    boolean requireUserInput;
    boolean requirePassword;
    boolean keyInput;
    String userChoiceTranslationKey;
    String passwordChoiceTranslationKey;

    public OptionsBuilder build() {
        var existing = identity.getValue();
        var user = new SimpleStringProperty(
                existing instanceof IdentityValue.InPlace inPlace && inPlace.unwrap() != null
                        ? inPlace.unwrap().getUsername()
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
        var options = new OptionsBuilder()
                .nameAndDescription(userChoiceTranslationKey)
                .addComp(new IdentitySelectComp(ref, user, pass, identityStrategy, allowCustomUserInput), user)
                .nonNullIf(inPlaceSelected.and(new SimpleBooleanProperty(requireUserInput)))
                .nameAndDescription(passwordChoiceTranslationKey)
                .sub(SecretRetrievalStrategyHelper.comp(pass, true), pass)
                .nonNullIf(inPlaceSelected.and(new SimpleBooleanProperty(requirePassword)))
                .disable(refSelected)
                .hide(refSelected)
                .addProperty(ref);
        if (keyInput) {
                options.name("keyAuthentication").description("keyAuthenticationDescription").longDescription("base:sshKey").sub(
                    SshIdentityStrategyHelper.identity(gateway != null ? gateway : new SimpleObjectProperty<>(), identityStrategy, path -> false, true),
                    identityStrategy).nonNullIf(inPlaceSelected).disable(refSelected).hide(
                    refSelected);
        }
               options.bind(
                        () -> {
                            if (ref.get() != null) {
                                return IdentityValue.Ref.builder()
                                        .ref(ref.get())
                                        .build();
                            } else {
                                return IdentityValue.InPlace.builder()
                                        .identityStore(LocalIdentityStore.builder()
                                                .username(user.get())
                                                .password(EncryptedValue.CurrentKey.of(pass.get()))
                                                .sshIdentity(keyInput ? EncryptedValue.CurrentKey.of(identityStrategy.get()) : EncryptedValue.CurrentKey.of(new SshIdentityStrategy.None()))
                                                .build())
                                        .build();
                            }
                        },
                        identity);
        return options;
    }
}
