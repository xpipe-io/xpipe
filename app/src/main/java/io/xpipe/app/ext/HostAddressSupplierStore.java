package io.xpipe.app.ext;

import io.xpipe.core.store.DataStore;

import java.util.List;

public interface HostAddressSupplierStore extends DataStore {

    List<HostAddress> getAllHostAddresses();

    HostAddress getDefaultHostAddress();
}
