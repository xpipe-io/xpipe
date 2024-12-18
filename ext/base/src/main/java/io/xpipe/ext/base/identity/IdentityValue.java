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

    void checkComplete(boolean requireUser) throws Throwable;

    IdentityStore getIdentityStore();

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
            identityStore.checkComplete();
            if (requireUser) {
                Validators.nonNull(identityStore.getUsername());
            }
        }

        @Override
        public LocalIdentityStore getIdentityStore() {
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
            ref.getStore().checkComplete();
            if (requireUser) {
                Validators.nonNull(ref.getStore().getUsername());
            }
        }

        @Override
        public IdentityStore getIdentityStore() {
            return ref.getStore();
        }

        @Override
        public boolean isPerUser() {
            return ref.getStore() instanceof SyncedIdentityStore s && s.isPerUser();
        }
    }
}
