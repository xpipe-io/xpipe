package io.xpipe.core.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Map;

@Value
@AllArgsConstructor(onConstructor_={@JsonCreator})
public class ConfigOptionSetInstance {

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
}
