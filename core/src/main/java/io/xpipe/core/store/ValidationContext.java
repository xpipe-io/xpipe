package io.xpipe.core.store;

public interface ValidationContext<T> {

    T get();

    void close();
}
