package io.xpipe.ext.base.host;

import io.xpipe.app.ext.NetworkTunnelStore;
import io.xpipe.app.storage.DataStoreEntryRef;

public interface HostAddressGatewayStore extends HostAddressStore {

    DataStoreEntryRef<NetworkTunnelStore> getGateway();
}
