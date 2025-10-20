package io.xpipe.ext.base.service;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.NetworkTunnelStore;
import io.xpipe.app.storage.DataStoreEntryRef;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.ext.base.host.AbstractHostStore;
import io.xpipe.ext.base.host.AbstractHostTransformStore;
import io.xpipe.ext.base.host.HostAddressStore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder(toBuilder = true)
@Getter
@Jacksonized
@JsonTypeName("customService")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class CustomServiceStore extends AbstractServiceStore implements AbstractHostTransformStore {

    private final DataStoreEntryRef<HostAddressStore> host;
    private final String address;
    private final DataStoreEntryRef<NetworkTunnelStore> gateway;


    @Override
    public boolean canConvertToAbstractHost() {
        return host == null;
    }

    @Override
    public AbstractHostStore createAbstractHostStore() {
        return AbstractHostStore.builder().host(address).gateway(gateway).build();
    }

    @Override
    public AbstractHostTransformStore withNewParent(DataStoreEntryRef<AbstractHostStore> newParent) {
        return toBuilder().address(null).gateway(null).host(newParent.asNeeded()).build();
    }
}
