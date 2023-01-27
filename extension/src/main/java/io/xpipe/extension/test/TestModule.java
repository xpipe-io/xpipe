package io.xpipe.extension.test;

import org.junit.jupiter.api.Named;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class TestModule<V> {

    private static final Map<Class<?>, Map<String, ?>> values = new HashMap<>();

    @SuppressWarnings({"unchecked"})
    public static <T> Map<String, T> get(Class<T> c, String... classes) {
        if (!values.containsKey(c)) {
            List<Class<?>> loadedClasses = (List<Class<?>>) Arrays.stream(classes)
                    .map(s -> {
                        try {
                            return Optional.of(Class.forName(s));
                        } catch (ClassNotFoundException ex) {
                            return Optional.empty();
                        }
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

    public static <T> Stream<Named<T>> getArguments(Class<T> c, String... classes) {
        Stream.Builder<Named<T>> argumentBuilder = Stream.builder();
        for (var s : TestModule.get(c, classes).entrySet()) {
            argumentBuilder.add(Named.of(s.getKey(), s.getValue()));
        }
        return argumentBuilder.build();
    }

    protected abstract void init(Map<String, Supplier<V>> list);

    protected abstract Class<V> getValueClass();
}
