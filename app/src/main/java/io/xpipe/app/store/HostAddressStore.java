package io.xpipe.app.store;

import io.xpipe.app.util.HostAddress;

public interface HostAddressStore extends DataStore {

    HostAddress getHostAddress();

    default void refreshHostAddressOrThrow() throws Exception {}
}
