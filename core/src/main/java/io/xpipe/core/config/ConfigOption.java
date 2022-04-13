package io.xpipe.core.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor(onConstructor_={@JsonCreator})
public class ConfigOption {
    String name;
    String key;
}
