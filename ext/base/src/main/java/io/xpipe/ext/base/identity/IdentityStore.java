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
    SecretRetrievalStrategy password;
    SshIdentityStrategy sshIdentity;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(password);
        Validators.nonNull(sshIdentity);
        sshIdentity.checkComplete();
    }
}
