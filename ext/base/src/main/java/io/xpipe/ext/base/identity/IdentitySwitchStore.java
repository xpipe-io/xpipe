package io.xpipe.ext.base.identity;

import io.xpipe.app.store.HostAddressStore;

public interface IdentitySwitchStore extends HostAddressStore {

    IdentityValue getIdentity();

    IdentitySwitchStore withIdentity(IdentityValue identity);
}
