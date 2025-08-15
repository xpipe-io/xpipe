package io.xpipe.app.ext;

import java.util.Optional;

public interface HostAddressSwitchStore extends DataStore {

    HostAddress getHostAddress();

    Optional<HostAddressSwitchStore> withAddress(String address);
}
