package io.xpipe.ext.base.host;

import io.xpipe.app.store.DataStore;
import io.xpipe.app.util.HostAddress;

public interface HostAddressStore extends DataStore {

    HostAddress getHostAddress();

    default void refreshHostAddressOrThrow() throws Exception {}
}
