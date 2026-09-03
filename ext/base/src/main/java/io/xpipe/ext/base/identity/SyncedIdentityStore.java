package io.xpipe.ext.base.identity;

import io.xpipe.app.identity.KeyFileStrategy;
import io.xpipe.app.identity.NoIdentityStrategy;
import io.xpipe.app.identity.SshIdentityStrategy;
import io.xpipe.app.identity.UsernameStrategy;
import io.xpipe.app.secret.OptionalEncryptedValue;
import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.storage.DataStoreAccessScope;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.util.ValidationException;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Objects;

@SuperBuilder
@JsonTypeName("syncedIdentity")
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString(callSuper = true)
@Jacksonized
public class SyncedIdentityStore extends IdentityStore {

    String username;
    OptionalEncryptedValue<SecretRetrievalStrategy> password;
    OptionalEncryptedValue<SshIdentityStrategy> sshIdentity;
    DataStoreAccessScope accessScope;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SyncedIdentityStore that)) {
            return false;
        }
        return Objects.equals(username, that.username)
                && Objects.equals(password, that.password)
                && Objects.equals(sshIdentity, that.sshIdentity)
                && Objects.equals(accessScope, that.accessScope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password, sshIdentity, accessScope);
    }

    public DataStoreAccessScope getAccessScopeRaw() {
        return accessScope;
    }

    @Override
    public DataStoreAccessScope getAccessScope() {
        return accessScope != null ? accessScope : DataStoreAccessScope.encryption();
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
    }

    OptionalEncryptedValue<SecretRetrievalStrategy> getEncryptedPassword() {
        return password;
    }

    OptionalEncryptedValue<SshIdentityStrategy> getEncryptedSshIdentity() {
        return sshIdentity;
    }

    @Override
    public DataStore withUpdatedPrincipals() {
        var targetScope = DataStoreAccessScope.getTargetScope(accessScope);
        if (targetScope != null && targetScope.equals(DataStoreAccessScope.vault())) {
            targetScope = null;
        }

        var newPassword = password != null ? password.withUpdatedPrincipals() : null;
        var newIdentity = sshIdentity != null ? sshIdentity.withUpdatedPrincipals() : null;

        return SyncedIdentityStore.builder()
                .username(username)
                .password(newPassword)
                .sshIdentity(newIdentity)
                .accessScope(targetScope)
                .build();
    }
}
