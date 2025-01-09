package io.xpipe.ext.base.identity;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.EncryptedValue;
import io.xpipe.app.util.SecretRetrievalStrategy;
import io.xpipe.app.util.Validators;

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

    static IdentityValue.InPlace of(LocalIdentityStore identityStore) {
        return new InPlace(identityStore);
    }

    static IdentityValue.InPlace empty() {
        return of(null, null,null);
    }

    static IdentityValue.InPlace of(String user) {
        return of(user, null,null);
    }

    static IdentityValue.InPlace of(String user, SecretRetrievalStrategy password) {
        return of(user, password,null);
    }

    static IdentityValue.InPlace of(String user, SecretRetrievalStrategy password, SshIdentityStrategy sshIdentity) {
        var s = LocalIdentityStore.builder().username(user)
                .password(EncryptedValue.of(password != null ? password : new SecretRetrievalStrategy.None()))
                .sshIdentity(EncryptedValue.of(sshIdentity != null ? sshIdentity : new SshIdentityStrategy.None()))
                .build();
        return of(s);
    }

    void checkComplete(boolean requireUser) throws Throwable;

    IdentityStore unwrap();

    boolean isPerUser();

    @JsonTypeName("inPlace")
    @Value
    @Jacksonized
    @Builder
    class InPlace implements IdentityValue {

        LocalIdentityStore identityStore;

        @Override
        public void checkComplete(boolean requireUser) throws Throwable {
            Validators.nonNull(identityStore);
            if (requireUser) {
                Validators.nonNull(identityStore.getUsername());
            }
            identityStore.checkComplete();
        }

        @Override
        public LocalIdentityStore unwrap() {
            return identityStore;
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
        public void checkComplete(boolean requireUser) throws Throwable {
            Validators.nonNull(ref);
            Validators.isType(ref, IdentityStore.class);
            if (requireUser) {
                Validators.nonNull(ref.getStore().getUsername());
            }
            ref.getStore().checkComplete();
        }

        @Override
        public IdentityStore unwrap() {
            return ref.getStore();
        }

        @Override
        public boolean isPerUser() {
            return ref.get().isPerUserStore();
        }
    }
}
