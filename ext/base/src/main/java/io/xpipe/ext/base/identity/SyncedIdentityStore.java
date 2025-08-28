package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.UserScopeStore;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.util.EncryptedValue;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.ext.base.identity.ssh.KeyFileStrategy;
import io.xpipe.ext.base.identity.ssh.SshIdentityStrategy;

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
    public void checkComplete() throws Throwable {
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
