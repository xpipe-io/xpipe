package io.xpipe.ext.base.host;

import io.xpipe.app.ext.*;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@ToString(callSuper = true)
@SuperBuilder
@Jacksonized
@JsonTypeName("abstractHost")
public class AbstractHostStore implements DataStore, HostAddressStore, HostAddressGatewayStore {

    String host;
    DataStoreEntryRef<NetworkTunnelStore> gateway;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(host);
    }

    @Override
    public HostAddress getHostAddress() {
        return HostAddress.of(host);
    }

    @Override
    public DataStoreEntryRef<NetworkTunnelStore> getTunnelGateway() {
        return gateway;
    }
}
