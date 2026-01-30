package io.xpipe.ext.base.service;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.FixedChildStore;
import io.xpipe.app.ext.NetworkTunnelStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.ext.base.host.HostAddressStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.OptionalInt;

@SuperBuilder(toBuilder = true)
@Getter
@Jacksonized
@JsonTypeName("fixedService")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FixedServiceStore extends AbstractServiceStore implements FixedChildStore {

    private final DataStoreEntryRef<HostAddressStore> host;
    private final DataStoreEntryRef<? extends DataStore> displayParent;
    private final Boolean tunnelToLocalhost;

    @Override
    public String getAddress() {
        return null;
    }

    @Override
    public DataStoreEntryRef<NetworkTunnelStore> getGateway() {
        return null;
    }

    @Override
    public DataStoreEntryRef<HostAddressStore> getHost() {
        return host;
    }

    @Override
    public boolean licenseRequired() {
        return false;
    }

    @Override
    public FixedChildStore merge(FixedChildStore other) {
        var o = (FixedServiceStore) other;
        return toBuilder().tunnelToLocalhost(o.tunnelToLocalhost).build();
    }

    @Override
    public void checkComplete() throws Throwable {
        super.checkComplete();
        Validators.nonNull(displayParent);
        Validators.nonNull(displayParent.getStore());
    }

    @Override
    public OptionalInt getFixedId() {
        return OptionalInt.of(getRemotePort());
    }

    @Override
    public boolean shouldTunnel() {
        return tunnelToLocalhost == null || tunnelToLocalhost;
    }
}
