package io.xpipe.extension;

import io.xpipe.beacon.exchange.NamedFunctionExchange;
import io.xpipe.extension.event.ErrorEvent;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

@Getter
public class NamedFunction {

    public static final List<NamedFunction> ALL = new ArrayList<>();

    public static void init(ModuleLayer layer) {
        if (ALL.size() == 0) {
            ALL.addAll(ServiceLoader.load(layer, NamedFunction.class).stream()
                    .map(p -> p.get())
                    .toList());
        }
    }

    public static NamedFunction get(String id) {
        return ALL.stream()
                .filter(namedFunction -> namedFunction.id.equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow();
    }

    public static <T> T callLocal(String id, Object... args) {
        return get(id).callLocal(args);
    }

    @SneakyThrows
    public static <T> T callRemote(String id, Object... args) {
        var proxy = XPipeProxy.getProxy(args[0]);
        var client = XPipeProxy.connect(proxy);
        client.sendRequest(
                NamedFunctionExchange.Request.builder().id(id).arguments(args).build());
        NamedFunctionExchange.Response response = client.receiveResponse();
        return (T) response.getReturnValue();
    }

    @SneakyThrows
    public static <T> T call(Class<? extends NamedFunction> clazz, Object... args) {
        var base = args[0];
        var proxy = XPipeProxy.getProxy(base);
        if (proxy != null) {
            return callRemote(clazz.getDeclaredConstructor().newInstance().getId(), args);
        } else {
            return callLocal(clazz.getDeclaredConstructor().newInstance().getId(), args);
        }
    }

    private final String id;
    private final Method method;

    public NamedFunction(String id, Method method) {
        this.id = id;
        this.method = method;
    }

    public NamedFunction(String id, Class<?> clazz) {
        this.id = id;
        this.method = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method1 -> method1.getName().equals(id))
                .findFirst()
                .orElseThrow();
    }

    public <T> T callLocal(Object... args) {
        try {
            return (T) method.invoke(null, args);
        } catch (Throwable ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return null;
        }
    }
}
