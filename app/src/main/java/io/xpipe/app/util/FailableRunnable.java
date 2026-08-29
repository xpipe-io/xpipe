package io.xpipe.app.util;

@FunctionalInterface
public interface FailableRunnable<E extends Throwable> {

    void run() throws E;
}
