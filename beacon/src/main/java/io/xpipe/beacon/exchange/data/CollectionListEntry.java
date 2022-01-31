package io.xpipe.beacon.exchange.data;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Value
@Jacksonized
@Builder
public class CollectionListEntry {

    String name;
    int size;
    Instant lastUsed;
}
