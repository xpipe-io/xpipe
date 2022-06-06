package io.xpipe.core.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value
@AllArgsConstructor(onConstructor_={@JsonCreator})
public class ConfigParameterSetInstance {

    /**
     * The available configuration parameters.
     */
    List<ConfigParameter> configParameters;

    /**
     * The current configuration options that are set.
     */
    Map<String, String> currentValues;

    public ConfigParameterSetInstance(Map<ConfigParameter, Object> map) {
        configParameters = map.keySet().stream().toList();
        currentValues = map.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().getKey(),
                e -> e.getKey().getConverter().convertToString(e.getValue())));
    }

    public <X, T extends Function<X,?>> ConfigParameterSetInstance(Map<ConfigParameter, T> map, Object v) {
        configParameters = map.keySet().stream().toList();
        currentValues = map.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().getKey(),
                e -> e.getKey().getConverter().convertToString(apply(e.getValue(), v))));
    }

    @SuppressWarnings("unchecked")
    private static <X, T extends Function<X,?>, V> Object apply(T func, Object v) {
        return func.apply((X) v);
    }

    public void update(ConfigParameter p, String val) {
        currentValues.put(p.getKey(), val);
    }

    public Map<ConfigParameter, Object> evaluate() {
        return configParameters.stream().collect(Collectors.toMap(
                p -> p,
                p -> p.getConverter().convertFromString(currentValues.get(p.getKey()))));
    }
}
