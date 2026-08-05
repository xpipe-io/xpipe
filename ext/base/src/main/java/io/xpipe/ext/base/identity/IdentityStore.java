package io.xpipe.ext.base.identity;

import io.xpipe.app.identity.SshIdentityStrategy;
import io.xpipe.app.identity.UsernameStrategy;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.store.SelfReferentialStore;
import io.xpipe.app.util.ValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Getter
public abstract class IdentityStore implements SelfReferentialStore, DataStore {

    public abstract String toSummary();

    public abstract DataStoreEntryRef<IdentityStore> getCustomEditTarget();

    public abstract UsernameStrategy getUsername();

    public abstract SecretRetrievalStrategy getPassword();

    public abstract SshIdentityStrategy getSshIdentity();

    @Override
    public void checkComplete() throws ValidationException {
        if (getPassword() != null) {
            getPassword().checkComplete();
        }
        if (getSshIdentity() != null) {
            getSshIdentity().checkComplete();
        }
    }

    public abstract String getName();
}
