package io.xpipe.ext.base.identity;

import io.xpipe.app.storage.DataStoreEntryRef;
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

    static IdentityValue of(LocalIdentityStore identityStore) {
        return new InPlace(identityStore);
    }

    void checkComplete(boolean requireUser, boolean requirePassword, boolean requireKey) throws Throwable;

    IdentityStore unwrap();

    boolean isPerUser();

    @JsonTypeName("inPlace")
    @Value
    @Jacksonized
    @Builder
    class InPlace implements IdentityValue {

        LocalIdentityStore identityStore;

        @Override
        public void checkComplete(boolean requireUser, boolean requirePassword, boolean requireKey) throws Throwable {
            Validators.nonNull(identityStore);
            identityStore.checkComplete();
            if (requireUser) {
                Validators.nonNull(identityStore.getUsername());
            }
            if (requirePassword) {
                Validators.nonNull(identityStore.getPassword());
            }
            if (requireKey) {
                Validators.nonNull(identityStore.getSshIdentity());
            }
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
        public void checkComplete(boolean requireUser, boolean requirePassword, boolean requireKey) throws Throwable {
            Validators.nonNull(ref);
            Validators.isType(ref, IdentityStore.class);
            ref.getStore().checkComplete();
            if (requireUser) {
                Validators.nonNull(ref.getStore().getUsername());
            }
            if (requirePassword) {
                Validators.nonNull(ref.getStore().getPassword());
            }
            if (requireKey) {
                Validators.nonNull(ref.getStore().getSshIdentity());
            }
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
