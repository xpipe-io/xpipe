package io.xpipe.core.util;

import lombok.SneakyThrows;

public abstract class SimpleProxyFunction<T> extends ProxyFunction {

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public T getResult() {
        var fields = getClass().getDeclaredFields();
        var last = fields[fields.length - 1];
        last.setAccessible(true);
        return (T) last.get(this);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public T callAndGet() {
        var result = callAndCopy();
        return ((SimpleProxyFunction<T>) result).getResult();
    }

}
