package io.xpipe.core.config;


import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
@AllArgsConstructor(onConstructor_={@JsonCreator})
public class ConfigOptionSet {

    public static ConfigOptionSet empty() {
        return new ConfigOptionSet(List.of());
    }

    @Singular
    List<ConfigOption> options;
}
