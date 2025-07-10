package io.xpipe.core;

@FunctionalInterface
public interface FailableRunnable<E extends Throwable> {

    void run() throws E;
}
