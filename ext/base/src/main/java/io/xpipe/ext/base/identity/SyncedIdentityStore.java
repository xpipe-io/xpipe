package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.UserScopeStore;
import io.xpipe.app.util.EncryptedValue;
import io.xpipe.app.util.SecretRetrievalStrategy;
import io.xpipe.app.util.Validators;
import io.xpipe.core.util.ValidationException;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@JsonTypeName("syncedIdentity")
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Jacksonized
public class SyncedIdentityStore extends IdentityStore implements UserScopeStore {

    // We can encrypt it with only the vault key as
    // per user stores are additionally encrypted on the entry level
    EncryptedValue.VaultKey<SecretRetrievalStrategy> password;
    EncryptedValue.VaultKey<SshIdentityStrategy> sshIdentity;
    boolean perUser;

    @Override
    public SecretRetrievalStrategy getPassword() {
        return password != null ? password.getValue() : null;
    }

    @Override
    public SshIdentityStrategy getSshIdentity() {
        return sshIdentity != null ? sshIdentity.getValue() : null;
    }

    @Override
    public void checkComplete() throws Throwable {
        super.checkComplete();
        if (getSshIdentity() instanceof SshIdentityStrategy.File f) {
            if (!f.getFile().isInDataDirectory()) {
                throw new ValidationException("Key file is not synced");
            }
        }
    }

    @Override
    EncryptedValue.VaultKey<SecretRetrievalStrategy> getEncryptedPassword() {
        return password;
    }

    @Override
    EncryptedValue.VaultKey<SshIdentityStrategy> getEncryptedSshIdentity() {
        return sshIdentity;
    }
}
