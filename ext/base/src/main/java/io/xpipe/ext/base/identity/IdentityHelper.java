package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.SecretRetrievalStrategyHelper;

import javafx.beans.property.*;

public class IdentityHelper {

    public static OptionsBuilder identity(
            Property<DataStoreEntryRef<ShellStore>> gateway,
            ObjectProperty<IdentityValue> identity,
            boolean allowUserInput) {
        var existing = identity.getValue();
        var user = new SimpleStringProperty(
                existing instanceof IdentityValue.InPlace inPlace
                        ? inPlace.getIdentityStore().getUsername()
                        : null);
        var pass = new SimpleObjectProperty<>(
                existing instanceof IdentityValue.InPlace inPlace
                        ? inPlace.getIdentityStore().getPassword()
                        : null);
        var identityStrategy = new SimpleObjectProperty<>(
                existing instanceof IdentityValue.InPlace inPlace
                        ? inPlace.getIdentityStore().getSshIdentity()
                        : null);
        var ref = new SimpleObjectProperty<>(existing instanceof IdentityValue.Ref r ? r.getRef() : null);
        var inPlaceSelected = ref.isNull();
        var refSelected = ref.isNotNull();
        var options = new OptionsBuilder()
                .nameAndDescription(allowUserInput ? "user" : "identityChoice")
                .addComp(new IdentityChoiceComp(ref, user, pass, identityStrategy, allowUserInput), user)
                .nonNullIf(inPlaceSelected.and(new SimpleBooleanProperty(allowUserInput)))
                .name("passwordAuthentication")
                .description("passwordDescription")
                .sub(SecretRetrievalStrategyHelper.comp(pass, true), pass)
                .nonNullIf(inPlaceSelected)
                .disable(refSelected)
                .hide(refSelected)
                .name("keyAuthentication")
                .description("keyAuthenticationDescription")
                .longDescription("base:sshKey")
                .sub(
                        SshIdentityStrategyHelper.identity(
                                gateway != null ? gateway : new SimpleObjectProperty<>(),
                                identityStrategy,
                                false,
                                null),
                        identityStrategy)
                .nonNullIf(inPlaceSelected)
                .disable(refSelected)
                .hide(refSelected)
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
