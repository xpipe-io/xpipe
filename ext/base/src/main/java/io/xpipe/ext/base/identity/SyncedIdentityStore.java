package io.xpipe.ext.base.identity;

import io.xpipe.app.identity.KeyFileStrategy;
import io.xpipe.app.identity.NoIdentityStrategy;
import io.xpipe.app.identity.SshIdentityStrategy;
import io.xpipe.app.identity.UsernameStrategy;
import io.xpipe.app.secret.EncryptedValue;
import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.storage.DataStoreAccessScope;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.AccessScopeStore;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.util.ValidationException;
import io.xpipe.app.util.Validators;

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
public class SyncedIdentityStore extends IdentityStore implements AccessScopeStore {

    String username;
    // We can encrypt it with only the vault key as
    // per user stores are additionally encrypted on the entry level
    EncryptedValue<SecretRetrievalStrategy> password;
    EncryptedValue<SshIdentityStrategy> sshIdentity;
    DataStoreAccessScope accessScope;

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
    public String getName() {
        return getSelfEntry().getName();
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
        Validators.nonNull(accessScope);
    }

    EncryptedValue<SecretRetrievalStrategy> getEncryptedPassword() {
        return password;
    }

    EncryptedValue<SshIdentityStrategy> getEncryptedSshIdentity() {
        return sshIdentity;
    }

    @Override
    public DataStore withUpdatedPrincipals() {
        return SyncedIdentityStore.builder()
                .username(username)
                .password(password != null ? password.withUpdatedPrincipals() : null)
                .sshIdentity(sshIdentity != null ? sshIdentity.withUpdatedPrincipals() : null)
                .accessScope(accessScope != null ? DataStoreAccessScope.getTargetScope(accessScope) : null)
                .build();
    }
}
