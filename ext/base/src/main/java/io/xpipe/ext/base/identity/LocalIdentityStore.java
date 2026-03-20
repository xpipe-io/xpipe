package io.xpipe.ext.base.identity;

import io.xpipe.app.cred.SshIdentityStrategy;
import io.xpipe.app.cred.UsernameStrategy;
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

@SuperBuilder(toBuilder = true)
@JsonTypeName("localIdentity")
@Jacksonized
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LocalIdentityStore extends IdentityStore {

    String username;
    EncryptedValue<SecretRetrievalStrategy> password;
    EncryptedValue<SshIdentityStrategy> sshIdentity;

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

    EncryptedValue<SecretRetrievalStrategy> getEncryptedPassword() {
        return password;
    }

    EncryptedValue<SshIdentityStrategy> getEncryptedSshIdentity() {
        return sshIdentity;
    }

    @Override
    public List<DataStoreEntryRef<?>> getDependencies() {
        return List.of();
    }
}
