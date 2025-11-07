package io.xpipe.ext.base.host;

import io.xpipe.app.ext.HostAddress;

import java.util.Optional;

public interface HostAddressSwitchStore extends HostAddressStore {

    HostAddress getHostAddress();

    Optional<HostAddressSwitchStore> withAddress(String address);
}
