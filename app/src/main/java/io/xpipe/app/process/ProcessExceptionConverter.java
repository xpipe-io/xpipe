package io.xpipe.app.process;

@FunctionalInterface
public interface ProcessExceptionConverter {

    Throwable convert(Throwable t);
}
