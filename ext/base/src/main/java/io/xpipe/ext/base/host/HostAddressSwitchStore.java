package io.xpipe.ext.base.host;

import io.xpipe.app.util.HostAddress;

import java.util.Optional;

public interface HostAddressSwitchStore extends io.xpipe.app.store.HostAddressStore {

    HostAddress getHostAddress();

    Optional<HostAddressSwitchStore> withAddress(String address);
}
