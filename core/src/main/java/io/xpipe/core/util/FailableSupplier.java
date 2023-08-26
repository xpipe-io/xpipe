package io.xpipe.core.util;

public interface FailableSupplier<T, E extends Throwable> {

    T get() throws E;
}
