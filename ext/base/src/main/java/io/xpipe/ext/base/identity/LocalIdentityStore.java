package io.xpipe.ext.base.identity;

import io.xpipe.app.beacon.BeaconAuthMethod;
import io.xpipe.app.identity.NoIdentityStrategy;
import io.xpipe.app.identity.SshIdentityStrategy;
import io.xpipe.app.identity.UsernameStrategy;
import io.xpipe.app.secret.EncryptedValue;
import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.storage.DataStoreAccessScope;
import io.xpipe.app.storage.DataStoreEntryRef;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.store.AccessScopeStore;
import io.xpipe.app.store.DataStore;
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
public class LocalIdentityStore extends IdentityStore implements AccessScopeStore {

    String username;
    EncryptedValue<SecretRetrievalStrategy> password;
    EncryptedValue<SshIdentityStrategy> sshIdentity;

    @Override
    public DataStoreAccessScope getAccessScope() {
        return DataStoreAccessScope.encryption();
    }

    @Override
    public DataStore withUpdatedPrincipals() {
        return LocalIdentityStore.builder()
                .username(username)
                .password(password != null ? password.withUpdatedPrincipals() : null)
                .sshIdentity(sshIdentity != null ? sshIdentity.withUpdatedPrincipals() : null)
                .build();
    }

    @Override
    public String toSummary() {
        var user = getUsername().hasUser()
                ? getUsername().getFixedUsername().map(s -> "User " + s).orElse("User")
                : "Anonymous user";
        var s = user
                + (getPassword() == null || getPassword() instanceof SecretNoneStrategy ? "" : " + password")
                + (getSshIdentity() == null || getSshIdentity() instanceof NoIdentityStrategy ? "" : " + key");
        return s;
    }

    @Override
    public DataStoreEntryRef<IdentityStore> getCustomEditTarget() {
        return null;
    }

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
    public String getName() {
        var inStorage = hasSelfEntry();
        return inStorage ? getSelfEntry().getName() : null;
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
