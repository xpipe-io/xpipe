package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.xpipe.core.config.ConfigParameter;
import io.xpipe.core.config.ConfigParameterSetInstance;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Map;
import java.util.function.Function;

/**
 * Represents the current configuration of a data source.
 * This configuration can either be in progress or complete.
 */
@Value
@AllArgsConstructor(onConstructor_={@JsonCreator})
public class DataSourceConfigInstance {

    public static DataSourceConfigInstance xpbt() {
        return new DataSourceConfigInstance("xpbt", new ConfigParameterSetInstance(Map.of()));
    }

    /**
     * The data source provider id.
     */
    String provider;

    /**
     * The available configuration parameters.
     */
    ConfigParameterSetInstance configInstance;

    public DataSourceConfigInstance(String provider, Map<ConfigParameter, Object> map) {
        this.provider = provider;
        this.configInstance = new ConfigParameterSetInstance(map);
    }

    public <X, T extends Function<X,?>> DataSourceConfigInstance(String provider, Map<ConfigParameter, T> map, Object val) {
        this.provider = provider;
        this.configInstance = new ConfigParameterSetInstance(map, val);
    }

    public Map<ConfigParameter, Object> evaluate() {
        return configInstance.evaluate();
    }
}
