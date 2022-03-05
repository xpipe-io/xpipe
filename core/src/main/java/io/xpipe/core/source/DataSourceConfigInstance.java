package io.xpipe.core.source;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * Represents the current configuration of a data source.
 * This configuration can either be in progress or complete.
 */
@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class DataSourceConfigInstance {

    public static DataSourceConfigInstance xpbt() {
        return new DataSourceConfigInstance("xpbt", DataSourceConfigOptions.empty(), Map.of());
    }

    /**
     * The data source provider id.
     */
    String provider;

    /**
     * The available configuration options.
     */
    DataSourceConfigOptions configOptions;

    /**
     * The current configuration options that are set.
     */
    Map<String, String> currentValues;

    public boolean isComplete() {
        return currentValues.size() == configOptions.getOptions().size();
    }
}
