package io.xpipe.ext.base.identity;

import io.xpipe.app.util.*;
import io.xpipe.core.store.*;
import io.xpipe.ext.base.SelfReferentialStore;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode
@ToString
@Getter
public abstract class IdentityStore implements SelfReferentialStore, DataStore {

    String username;

    public abstract SecretRetrievalStrategy getPassword();

    public abstract SshIdentityStrategy getSshIdentity();

    @Override
    public void checkComplete() throws Throwable {
        if (getPassword() != null) {
            getPassword().checkComplete();
        }
        if (getSshIdentity() != null) {
            getSshIdentity().checkComplete();
        }
    }

    abstract EncryptedValue<SecretRetrievalStrategy> getEncryptedPassword();

    abstract EncryptedValue<SshIdentityStrategy> getEncryptedSshIdentity();
}
