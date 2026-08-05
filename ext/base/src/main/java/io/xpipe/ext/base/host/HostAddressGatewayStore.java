package io.xpipe.ext.base.host;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.NetworkTunnelStore;

public interface HostAddressGatewayStore extends HostAddressStore {

    DataStoreEntryRef<NetworkTunnelStore> getTunnelGateway();
}
