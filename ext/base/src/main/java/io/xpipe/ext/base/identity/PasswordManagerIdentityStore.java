package io.xpipe.ext.base.identity;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.EncryptedValue;
import io.xpipe.app.util.SecretQuery;
import io.xpipe.app.util.SecretRetrievalStrategy;
import io.xpipe.core.store.InternalCacheDataStore;
import io.xpipe.core.store.StatefulDataStore;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@JsonTypeName("passwordManagerIdentity")
@Jacksonized
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PasswordManagerIdentityStore extends IdentityStore implements InternalCacheDataStore {

    String key;

    @Override
    public String getUsername() {
        return "";
    }

    @Override
    public SecretRetrievalStrategy getPassword() {
        return new SecretRetrievalStrategy() {

            @Override
            public SecretQuery query() {
                return null;
            }
        };
    }

    @Override
    public SshIdentityStrategy getSshIdentity() {
        return null;
    }
}
