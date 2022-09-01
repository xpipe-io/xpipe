package io.xpipe.api;

import java.util.Map;

/**
 * Represents the current configuration of a data source.
 */
public final class DataSourceConfig {

    /**
     * The data source provider id.
     */
    private final String provider;

    /**
     * The set configuration parameters.
     */
    private final Map<String, String> configInstance;

    public DataSourceConfig(String provider, Map<String, String> configInstance) {
        this.provider = provider;
        this.configInstance = configInstance;
    }

    public String getProvider() {
        return provider;
    }

    public Map<String, String> getConfig() {
        return configInstance;
    }
}
