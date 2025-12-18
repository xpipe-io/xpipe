package io.xpipe.ext.base.identity;

import io.xpipe.ext.base.host.HostAddressStore;

public interface IdentitySwitchStore extends HostAddressStore {

    IdentityValue getIdentity();

    IdentitySwitchStore withIdentity(IdentityValue identity);
}
