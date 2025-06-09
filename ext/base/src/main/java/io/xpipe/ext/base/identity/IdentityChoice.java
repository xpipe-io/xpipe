package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.EncryptedValue;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.SecretRetrievalStrategyHelper;

import javafx.beans.property.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class IdentityChoice {

    public static OptionsBuilder ssh(
            Property<DataStoreEntryRef<ShellStore>> host, ObjectProperty<IdentityValue> identity, boolean requireUser) {
        var i = new IdentityChoice(
                host, identity, true, requireUser, true, true, true, "identityChoice", "passwordAuthentication");
        return i.build();
    }

    public static OptionsBuilder container(ObjectProperty<IdentityValue> identity) {
        var i = new IdentityChoice(
                null, identity, true, false, false, false, false, "customUsername", "customUsernamePassword");
        return i.build();
    }

    Property<DataStoreEntryRef<ShellStore>> host;
    ObjectProperty<IdentityValue> identity;
    boolean allowCustomUserInput;
    boolean requireUserInput;
    boolean requirePassword;
    boolean keyInput;
    boolean allowAgentForward;
    String userChoiceTranslationKey;
    String passwordChoiceTranslationKey;

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
            options.name("keyAuthentication")
                    .description("keyAuthenticationDescription")
                    .longDescription("base:sshKey")
                    .sub(
                            SshIdentityStrategyHelper.identity(
                                    host != null
                                            ? host
                                            : new ReadOnlyObjectWrapper<>(
                                                    DataStorage.get().local().ref()),
                                    identityStrategy,
                                    path -> false,
                                    true,
                                    allowAgentForward),
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
