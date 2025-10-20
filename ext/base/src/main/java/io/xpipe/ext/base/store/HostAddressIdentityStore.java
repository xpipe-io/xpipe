package io.xpipe.ext.base.store;

import io.xpipe.app.ext.HostAddressStore;
import io.xpipe.ext.base.identity.IdentityValue;

public interface HostAddressIdentityStore extends HostAddressStore {

    IdentityValue getIdentity();
}
