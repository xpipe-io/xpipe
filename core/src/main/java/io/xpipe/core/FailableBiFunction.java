package io.xpipe.core;

@FunctionalInterface
public interface FailableBiFunction<T1, T2, R, E extends Throwable> {

    R apply(T1 var1, T2 var2) throws E;
}
