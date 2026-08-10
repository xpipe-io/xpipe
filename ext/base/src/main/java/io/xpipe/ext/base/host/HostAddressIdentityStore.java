package io.xpipe.ext.base.host;

import io.xpipe.ext.base.identity.IdentityValue;

public interface HostAddressIdentityStore extends io.xpipe.app.store.HostAddressStore {

    IdentityValue getIdentity();
}
