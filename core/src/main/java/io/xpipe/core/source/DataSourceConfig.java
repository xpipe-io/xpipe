package io.xpipe.core.source;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class DataSourceConfig {
    String description;

    @Singular
    List<Option> options;

    @Value
    @Builder
    @Jacksonized
    @AllArgsConstructor
    public static class Option {
        String name;
        String key;
    }
}
