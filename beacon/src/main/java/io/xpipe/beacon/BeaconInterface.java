package io.xpipe.beacon;

import io.xpipe.core.util.ModuleLayerLoader;

import com.sun.net.httpserver.HttpExchange;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public abstract class BeaconInterface<T> {

    private static List<BeaconInterface<?>> ALL;

    public static List<BeaconInterface<?>> getAll() {
        return ALL;
    }

    public static Optional<BeaconInterface<?>> byPath(String path) {
        return ALL.stream().filter(d -> d.getPath().equals(path)).findAny();
    }

    public static <RQ> Optional<BeaconInterface<?>> byRequest(RQ req) {
        return ALL.stream()
                .filter(d -> d.getRequestClass().equals(req.getClass()))
                .findAny();
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            var services = layer != null
                    ? ServiceLoader.load(layer, BeaconInterface.class)
                    : ServiceLoader.load(BeaconInterface.class);
            ALL = services.stream()
                    .map(ServiceLoader.Provider::get)
                    .map(beaconInterface -> (BeaconInterface<?>) beaconInterface)
                    .collect(Collectors.toList());
            // Remove parent classes
            ALL.removeIf(beaconInterface -> ALL.stream()
                    .anyMatch(other -> !other.equals(beaconInterface)
                            && beaconInterface.getClass().isAssignableFrom(other.getClass())));
        }
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public Class<T> getRequestClass() {
        var c = getClass().getSuperclass();
        var name = (c.getSuperclass().equals(BeaconInterface.class) ? c : getClass()).getName() + "$Request";
        return (Class<T>) Class.forName(name);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public Class<T> getResponseClass() {
        var c = getClass().getSuperclass();
        var name = (c.getSuperclass().equals(BeaconInterface.class) ? c : getClass()).getName() + "$Response";
        return (Class<T>) Class.forName(name);
    }

    public boolean requiresCompletedStartup() {
        return true;
    }

    public boolean requiresAuthentication() {
        return true;
    }

    public abstract String getPath();

    public Object handle(HttpExchange exchange, T body) throws Throwable {
        throw new UnsupportedOperationException();
    }

    public boolean readRawRequestBody() {
        return false;
    }
}
