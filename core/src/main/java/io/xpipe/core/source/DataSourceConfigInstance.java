package io.xpipe.core.source;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class DataSourceConfigInstance {

    String provider;
    DataSourceConfig config;
    Map<String, String> currentValues;
}
