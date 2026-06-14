package io.xpipe.app.beacon;

import io.xpipe.app.util.ModuleLayerLoader;

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

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public Class<T> getRequestClass() {
        var name = getClass().getName() + "$Request";
        return (Class<T>) Class.forName(name);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public Class<T> getResponseClass() {
        var name = getClass().getName() + "$Response";
        return (Class<T>) Class.forName(name);
    }

    public boolean acceptInShutdown() {
        return false;
    }

    public boolean requiresCompletedStartup() {
        return true;
    }

    public boolean requiresAuthentication() {
        return true;
    }

    public abstract String getPath();

    public List<String> getPathAliases() {
        return List.of();
    }

    public Object handle(HttpExchange exchange, T body) throws Throwable {
        throw new UnsupportedOperationException();
    }

    public boolean readRawRequestBody() {
        return false;
    }

    public boolean requiresBody() {
        return true;
    }

    public boolean requiresEnabledApi() {
        return true;
    }

    public Object getSynchronizationObject() {
        return null;
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            var services = ServiceLoader.load(layer, BeaconInterface.class);
            ALL = services.stream()
                    .map(ServiceLoader.Provider::get)
                    .map(beaconInterface -> (BeaconInterface<?>) beaconInterface)
                    .collect(Collectors.toList());
        }

        @Override
        public boolean initForCli() {
            return true;
        }
    }
}
