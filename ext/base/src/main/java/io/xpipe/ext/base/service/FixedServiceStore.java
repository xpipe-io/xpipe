package io.xpipe.ext.base.service;

import io.xpipe.app.ext.NetworkTunnelStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.FixedChildStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.OptionalInt;

@SuperBuilder
@Getter
@Jacksonized
@JsonTypeName("fixedService")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FixedServiceStore extends AbstractServiceStore implements FixedChildStore {

    private final DataStoreEntryRef<NetworkTunnelStore> host;
    private final DataStoreEntryRef<? extends DataStore> displayParent;

    @Override
    public boolean licenseRequired() {
        return false;
    }

    @Override
    public void checkComplete() throws Throwable {
        super.checkComplete();
        Validators.nonNull(displayParent);
        Validators.nonNull(displayParent.getStore());
    }

    @Override
    public DataStoreEntryRef<NetworkTunnelStore> getHost() {
        return host;
    }

    @Override
    public OptionalInt getFixedId() {
        return OptionalInt.of(getRemotePort());
    }
}
