package io.xpipe.ext.base.identity;

import io.xpipe.app.cred.SshIdentityStrategy;
import io.xpipe.app.cred.UsernameStrategy;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.SelfReferentialStore;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.secret.SecretRetrievalStrategy;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Getter
public abstract class IdentityStore implements SelfReferentialStore, DataStore {

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
}
