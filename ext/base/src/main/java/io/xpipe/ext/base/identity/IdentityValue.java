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
