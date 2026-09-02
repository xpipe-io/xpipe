package io.xpipe.ext.base.identity;

import io.xpipe.app.identity.NoIdentityStrategy;
import io.xpipe.app.identity.SshIdentityStrategy;
import io.xpipe.app.secret.OptionalEncryptedValue;
import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.storage.*;
import io.xpipe.app.store.DataStoreDependencies;
import io.xpipe.app.util.ValidationException;
import io.xpipe.app.util.Validators;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

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
                .password(OptionalEncryptedValue.of(new SecretNoneStrategy(), DataStoreAccessScope.encryption()))
                .sshIdentity(OptionalEncryptedValue.of(new NoIdentityStrategy(), DataStoreAccessScope.encryption()))
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
                .password(password != null ? OptionalEncryptedValue.of(password, DataStoreAccessScope.encryption()) : null)
                .sshIdentity(
                        sshIdentity != null ? OptionalEncryptedValue.of(sshIdentity, DataStoreAccessScope.encryption()) : null)
                .build();
        return of(s);
    }

    void checkComplete() throws ValidationException;

    IdentityStore unwrap();

    DataStoreAccessScope getScope();

    boolean isInPlace();

    List<DataStoreEntryRef<?>> getDependencies();

    IdentityValue withUpdatedPrincipals();

    default void checkCompleteUser() throws ValidationException {
        var n = unwrap().getName();
        var msg = n != null ? "Username of identity " + n : "Identity username";
        Validators.nonNull(unwrap().getUsername().hasUser() ? new Object() : null, msg);
    }

    default void checkCompletePassword() throws ValidationException {
        var n = unwrap().getName();
        var msg = n != null ? "Password of identity " + n : "Identity password";
        Validators.nonNull(unwrap().getPassword(), msg);
        unwrap().getPassword().checkComplete();
    }

    default void checkCompleteSshIdentity() throws ValidationException {
        var n = unwrap().getName();
        var msg = n != null ? "SSH key of identity " + n : "Identity SSH key";
        Validators.nonNull(unwrap().getSshIdentity(), msg);
        unwrap().getSshIdentity().checkComplete();
    }

    @JsonTypeName("inPlace")
    @Value
    @Jacksonized
    @Builder
    class InPlace implements IdentityValue {

        LocalIdentityStore identityStore;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(identityStore);
        }

        @Override
        public LocalIdentityStore unwrap() {
            return identityStore != null
                    ? identityStore
                    : LocalIdentityStore.builder().build();
        }

        @Override
        public DataStoreAccessScope getScope() {
            return DataStoreAccessScope.encryption();
        }

        @Override
        public boolean isInPlace() {
            return true;
        }

        @Override
        public List<DataStoreEntryRef<?>> getDependencies() {
            return List.of();
        }

        @Override
        public IdentityValue withUpdatedPrincipals() {
            return identityStore != null ? new InPlace(identityStore.withUpdatedPrincipals()) : this;
        }
    }

    @JsonTypeName("ref")
    @Value
    @Jacksonized
    @Builder
    class Ref implements IdentityValue {

        DataStoreEntryRef<IdentityStore> ref;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(ref);
            Validators.isType(ref, IdentityStore.class);
            ref.getStore().checkComplete();
        }

        @Override
        public IdentityStore unwrap() {
            return ref != null && ref.getStore() != null
                    ? ref.getStore()
                    : LocalIdentityStore.builder().build();
        }

        @Override
        public DataStoreAccessScope getScope() {
            return ref != null ? ref.get().getAccessScope() : DataStoreAccessScope.encryption();
        }

        @Override
        public boolean isInPlace() {
            return false;
        }

        @Override
        public List<DataStoreEntryRef<?>> getDependencies() {
            return DataStoreDependencies.of(ref);
        }

        @Override
        public IdentityValue withUpdatedPrincipals() {
            return this;
        }
    }
}
