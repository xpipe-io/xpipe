package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.SecretRetrievalStrategyHelper;

import javafx.beans.property.*;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
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
                .sub(SecretRetrievalStrategyHelper.comp(pass, true, true), pass)
                .nonNullIf(inPlaceSelected.and(new SimpleBooleanProperty(requirePassword)))
                .disable(refSelected)
                .hide(refSelected)
                .name("keyAuthentication")
                .description("keyAuthenticationDescription")
                .longDescription("base:sshKey")
                .sub(
                        SshIdentityStrategyHelper.identity(
                                gateway != null ? gateway : new SimpleObjectProperty<>(),
                                identityStrategy,
                                null,
                                false,
                                true),
                        identityStrategy)
                .nonNullIf(inPlaceSelected.and(new SimpleBooleanProperty(keyInput)))
                .disable(refSelected)
                .hide(refSelected.or(new SimpleBooleanProperty(!keyInput)))
                .addProperty(ref)
                .bind(
                        () -> {
                            if (ref.get() != null) {
                                return IdentityValue.Ref.builder()
                                        .ref(ref.get())
                                        .build();
                            } else {
                                return IdentityValue.InPlace.builder()
                                        .identityStore(LocalIdentityStore.builder()
                                                .username(user.get())
                                                .password(pass.get())
                                                .sshIdentity(identityStrategy.get())
                                                .build())
                                        .build();
                            }
                        },
                        identity);
        return options;
    }
}
