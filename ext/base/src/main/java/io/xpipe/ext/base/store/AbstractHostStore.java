package io.xpipe.ext.base.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.ext.*;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.ext.base.identity.IdentityValue;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@ToString(callSuper = true)
@SuperBuilder
@Jacksonized
@JsonTypeName("abstractHost")
public class AbstractHostStore implements DataStore, HostAddressStore {

    String host;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(host);
    }

    @Override
    public HostAddress getHostAddress() {
        return HostAddress.of(host);
    }
}
