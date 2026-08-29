package io.xpipe.app.util;

public interface FailableSupplier<T> {

    T get() throws Exception;
}
