package io.xpipe.ext.base.service;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.NetworkTunnelStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@Getter
@Jacksonized
@JsonTypeName("customService")
public final class CustomServiceStore extends AbstractServiceStore {

    private final DataStoreEntryRef<NetworkTunnelStore> host;
}
