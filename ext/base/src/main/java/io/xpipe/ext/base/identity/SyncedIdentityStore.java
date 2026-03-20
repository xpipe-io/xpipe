package io.xpipe.ext.base.identity;

import io.xpipe.app.cred.KeyFileStrategy;
import io.xpipe.app.cred.SshIdentityStrategy;
import io.xpipe.app.cred.UsernameStrategy;
import io.xpipe.app.ext.UserScopeStore;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.secret.EncryptedValue;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.storage.DataStoreEntryRef;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@SuperBuilder
@JsonTypeName("syncedIdentity")
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Jacksonized
public class SyncedIdentityStore extends IdentityStore implements UserScopeStore {

    String username;
    // We can encrypt it with only the vault key as
    // per user stores are additionally encrypted on the entry level
    EncryptedValue.VaultKey<SecretRetrievalStrategy> password;
    EncryptedValue.VaultKey<SshIdentityStrategy> sshIdentity;
    boolean perUser;

    public UsernameStrategy.Fixed getUsername() {
        return new UsernameStrategy.Fixed(username);
    }

    @Override
    public SecretRetrievalStrategy getPassword() {
        return password != null ? password.getValue() : null;
    }

    @Override
    public SshIdentityStrategy getSshIdentity() {
        return sshIdentity != null ? sshIdentity.getValue() : null;
    }

    @Override
    public List<DataStoreEntryRef<?>> getDependencies() {
        return List.of();
    }

    @Override
    public void checkComplete() throws ValidationException {
        super.checkComplete();
        if (getSshIdentity() instanceof KeyFileStrategy f) {
            if (!f.getFile().isInDataDirectory()) {
                throw new ValidationException("Key file is not synced");
            }
        }
    }

    EncryptedValue.VaultKey<SecretRetrievalStrategy> getEncryptedPassword() {
        return password;
    }

    EncryptedValue.VaultKey<SshIdentityStrategy> getEncryptedSshIdentity() {
        return sshIdentity;
    }
}
