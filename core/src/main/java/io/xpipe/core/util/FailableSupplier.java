package io.xpipe.core.util;

public interface FailableSupplier<T> {

    T get() throws Exception;
}
