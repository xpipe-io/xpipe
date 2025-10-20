package io.xpipe.ext.base.host;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.HostAddress;
import io.xpipe.ext.base.identity.IdentityValue;

public interface HostStore extends DataStore {

    HostAddress getHostAddress();

    IdentityValue getIdentity();
}
