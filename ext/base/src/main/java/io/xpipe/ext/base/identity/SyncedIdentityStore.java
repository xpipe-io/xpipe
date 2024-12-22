package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.UserScopeStore;
import io.xpipe.core.util.ValidationException;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@JsonTypeName("syncedIdentity")
@Jacksonized
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SyncedIdentityStore extends IdentityStore implements UserScopeStore {

    boolean perUser;

    @Override
    public void checkComplete() throws Throwable {
        super.checkComplete();
        if (getSshIdentity() instanceof SshIdentityStrategy.File f) {
            if (!f.getFile().isInDataDirectory()) {
                throw new ValidationException("Key file is not synced");
            }
        }
    }
}
