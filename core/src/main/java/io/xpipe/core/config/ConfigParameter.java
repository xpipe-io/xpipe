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
    QueryConverter<?> converter;

    @SuppressWarnings("unchecked")
    public <T> QueryConverter<T> getConverter() {
        return (QueryConverter<T>) converter;
    }
}
