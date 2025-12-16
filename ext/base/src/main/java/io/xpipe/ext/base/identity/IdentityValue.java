package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.secret.EncryptedValue;
import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.ext.base.identity.ssh.NoIdentityStrategy;
import io.xpipe.ext.base.identity.ssh.SshIdentityStrategy;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = IdentityValue.InPlace.class),
    @JsonSubTypes.Type(value = IdentityValue.Ref.class)
})
public interface IdentityValue {

    static IdentityValue ofCategory(DataStoreCategory category) {
        var effective = DataStorage.get().getEffectiveCategoryConfig(category);
        if (effective.getDefaultIdentityStore() == null) {
            return null;
        }

        var found = DataStorage.get().getStoreEntryIfPresent(effective.getDefaultIdentityStore());
        if (found.isEmpty() || !(found.get().getStore() instanceof IdentityStore)) {
            return null;
        }

        return new Ref(found.get().ref());
    }

    static IdentityValue ofBreakout(DataStoreEntry e) {
        var s = DataStorage.get();
        if (s == null) {
            return null;
        }

        var cat = s.getStoreCategory(e);
        var uuid = cat.getConfig().getDefaultIdentityStore();
        var found = s.getStoreEntryIfPresent(uuid);
        if (found.isEmpty() || !(found.get().getStore() instanceof IdentityStore)) {
            return null;
        }

        return new Ref(found.get().ref());
    }

    static IdentityValue.InPlace of(LocalIdentityStore identityStore) {
        return new InPlace(identityStore);
    }

    static IdentityValue.InPlace none() {
        var s = LocalIdentityStore.builder()
                .password(EncryptedValue.of(new SecretNoneStrategy()))
                .sshIdentity(EncryptedValue.of(new NoIdentityStrategy()))
                .build();
        return of(s);
    }

    static IdentityValue.InPlace of(String user) {
        return of(user, null, null);
    }

    static IdentityValue.InPlace of(String user, SecretRetrievalStrategy password) {
        return of(user, password, null);
    }

    static IdentityValue.InPlace of(String user, SecretRetrievalStrategy password, SshIdentityStrategy sshIdentity) {
        var s = LocalIdentityStore.builder()
                .username(user)
                .password(password != null ? EncryptedValue.of(password) : null)
                .sshIdentity(sshIdentity != null ? EncryptedValue.of(sshIdentity) : null)
                .build();
        return of(s);
    }

    void checkComplete() throws Throwable;

    IdentityStore unwrap();

    boolean isPerUser();

    default void checkCompleteUser() throws ValidationException {
        Validators.nonNull(unwrap().getUsername().hasUser() ? new Object() : null, "Identity username");
    }

    default void checkCompletePassword() throws ValidationException {
        Validators.nonNull(unwrap().getPassword(), "Identity password");
        unwrap().getPassword().checkComplete();
    }

    default void checkCompleteSshIdentity() throws ValidationException {
        Validators.nonNull(unwrap().getSshIdentity(), "Identity ssh key");
        unwrap().getSshIdentity().checkComplete();
    }

    @JsonTypeName("inPlace")
    @Value
    @Jacksonized
    @Builder
    class InPlace implements IdentityValue {

        LocalIdentityStore identityStore;

        @Override
        public void checkComplete() throws Throwable {
            Validators.nonNull(identityStore);
        }

        @Override
        public LocalIdentityStore unwrap() {
            return identityStore != null
                    ? identityStore
                    : LocalIdentityStore.builder().build();
        }

        @Override
        public boolean isPerUser() {
            return false;
        }
    }

    @JsonTypeName("ref")
    @Value
    @Jacksonized
    @Builder
    class Ref implements IdentityValue {

        DataStoreEntryRef<IdentityStore> ref;

        @Override
        public void checkComplete() throws Throwable {
            Validators.nonNull(ref);
            Validators.isType(ref, IdentityStore.class);
        }

        @Override
        public IdentityStore unwrap() {
            return ref != null && ref.getStore() != null
                    ? ref.getStore()
                    : LocalIdentityStore.builder().build();
        }

        @Override
        public boolean isPerUser() {
            return ref != null && ref.get().isPerUserStore();
        }
    }
}
