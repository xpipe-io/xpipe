package io.xpipe.ext.base.host;

import io.xpipe.ext.base.identity.IdentityValue;

public interface HostAddressIdentityStore extends HostAddressStore {

    IdentityValue getIdentity();
}
