package io.xpipe.beacon.exchange.data;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
public class ProviderEntry {
    String id;
    String description;
    boolean hidden;
}
