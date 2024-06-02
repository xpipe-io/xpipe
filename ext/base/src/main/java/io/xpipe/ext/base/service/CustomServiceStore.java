package io.xpipe.ext.base.service;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.NetworkTunnelStore;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@Getter
@Jacksonized
@JsonTypeName("service")
public class CustomServiceStore extends AbstractServiceStore {

    private final DataStoreEntryRef<NetworkTunnelStore> host;
}
