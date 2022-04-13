package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.xpipe.core.config.ConfigOptionSet;
import io.xpipe.core.config.ConfigOptionSetInstance;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Map;

/**
 * Represents the current configuration of a data source.
 * This configuration can either be in progress or complete.
 */
@Value
@AllArgsConstructor(onConstructor_={@JsonCreator})
public class DataSourceConfigInstance {

    public static DataSourceConfigInstance xpbt() {
        return new DataSourceConfigInstance("xpbt", ConfigOptionSet.empty(), Map.of());
    }

    /**
     * The data source provider id.
     */
    String provider;

    /**
     * The available configuration options.
     */
    ConfigOptionSet configOptions;

    /**
     * The current configuration options that are set.
     */
    Map<String, String> currentValues;

    public boolean isComplete() {
        return currentValues.size() == configOptions.getOptions().size();
    }

    public DataSourceConfigInstance(String provider, ConfigOptionSetInstance cInstance) {
        this.provider = provider;
        this.configOptions = cInstance.getConfigOptions();
        this.currentValues = cInstance.getCurrentValues();
    }
}
