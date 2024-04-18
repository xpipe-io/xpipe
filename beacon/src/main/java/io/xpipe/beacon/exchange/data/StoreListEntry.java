package io.xpipe.beacon.exchange.data;

import io.xpipe.core.store.DataStoreId;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
public class StoreListEntry {

    DataStoreId id;
    String type;
    String information;
}
