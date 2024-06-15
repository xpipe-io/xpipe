package io.xpipe.ext.base.service;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedChildStore;
import io.xpipe.core.store.NetworkTunnelStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.OptionalInt;

@SuperBuilder
@Getter
@Jacksonized
@JsonTypeName("fixedService")
public class FixedServiceStore extends AbstractServiceStore implements FixedChildStore {

    private final DataStoreEntryRef<NetworkTunnelStore> host;
    private final DataStoreEntryRef<? extends DataStore> displayParent;

    @Override
    public DataStoreEntryRef<NetworkTunnelStore> getHost() {
        return host;
    }

    @Override
    public OptionalInt getFixedId() {
        return OptionalInt.of(getRemotePort());
    }
}
