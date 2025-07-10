package io.xpipe.core;

public interface FailableSupplier<T> {

    T get() throws Exception;
}
