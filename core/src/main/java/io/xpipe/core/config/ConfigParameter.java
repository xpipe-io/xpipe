package io.xpipe.core.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class ConfigParameter {
    String key;

    @JsonCreator
    public ConfigParameter(String key) {
        this.key = key;
        this.converter = null;
    }

    @JsonIgnore
    ConfigConverter<?> converter;

    @SuppressWarnings("unchecked")
    public <T> ConfigConverter<T> getConverter() {
        return (ConfigConverter<T>) converter;
    }
}
