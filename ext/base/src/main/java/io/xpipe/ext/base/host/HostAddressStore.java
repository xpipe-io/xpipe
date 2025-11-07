package io.xpipe.ext.base.host;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.HostAddress;

public interface HostAddressStore extends DataStore {

    HostAddress getHostAddress();
}
