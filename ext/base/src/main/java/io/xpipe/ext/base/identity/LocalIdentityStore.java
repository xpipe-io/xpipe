package io.xpipe.ext.base.identity;

import io.xpipe.app.util.EncryptedValue;
import io.xpipe.app.util.SecretRetrievalStrategy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
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
}
