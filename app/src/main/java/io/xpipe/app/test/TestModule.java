package io.xpipe.app.test;

import org.junit.jupiter.api.Named;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class TestModule<V> {

    private static final Map<Class<?>, Map<String, ?>> values = new LinkedHashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
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

        return (Map<String, T>) values.get(c).entrySet().stream()
                .collect(Collectors.toMap(o -> o.getKey(), o -> ((Supplier<?>) o.getValue()).get()));
    }

    public static <T> Stream<Named<T>> getArguments(Class<T> c, Module module, String... classes) {
        Stream.Builder<Named<T>> argumentBuilder = Stream.builder();
        for (var s : TestModule.get(c, module, classes).entrySet()) {
            argumentBuilder.add(Named.of(s.getKey(), s.getValue()));
        }
        return argumentBuilder.build();
    }

    protected abstract void init(Map<String, Supplier<V>> list) throws Exception;

    protected abstract Class<V> getValueClass();
}
