package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.HostAddress;
import io.xpipe.ext.base.host.HostAddressStore;

import java.util.Optional;

public interface IdentitySwitchStore extends HostAddressStore {

    IdentityValue getIdentity();

    IdentitySwitchStore withIdentity(IdentityValue identity);
}
