package io.xpipe.app.test;

import io.xpipe.core.util.FailableSupplier;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Named;

import java.util.*;
import java.util.stream.Stream;

public abstract class TestModule<V> {

    private static final Map<Class<?>, Map<String, ?>> values = new LinkedHashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SneakyThrows
    public static <T> Map<String, T> get(Class<T> c, Module module, String... classes) {
        if (!values.containsKey(c)) {
            List<Class<?>> loadedClasses = Arrays.stream(classes)
                    .map(s -> {
                        return Optional.<Class<?>>of(Class.forName(module, s));
                    })
                    .flatMap(Optional::stream)
                    .toList();
            loadedClasses.forEach(o -> {
                try {
                    var instance = (TestModule<?>) o.getConstructor().newInstance();
                    Map list = values.computeIfAbsent(instance.getValueClass(), aClass -> new LinkedHashMap());
                    instance.init(list);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, ?> o : values.get(c).entrySet()) {
            if (map.put(o.getKey(), ((FailableSupplier<?>) o.getValue()).get()) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }
        return (Map<String, T>) map;
    }

    public static <T> Stream<Named<T>> getArguments(Class<T> c, Module module, String... classes) {
        Stream.Builder<Named<T>> argumentBuilder = Stream.builder();
        for (var s : TestModule.get(c, module, classes).entrySet()) {
            argumentBuilder.add(Named.of(s.getKey(), s.getValue()));
        }
        return argumentBuilder.build();
    }

    protected abstract void init(Map<String, FailableSupplier<V>> list) throws Exception;

    protected abstract Class<V> getValueClass();
}
