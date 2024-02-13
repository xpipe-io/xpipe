package io.xpipe.core.process;

import io.xpipe.core.util.FailableFunction;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public interface ProcessControl extends AutoCloseable {

    @FunctionalInterface
    interface ExceptionConverter {
        <T extends Throwable> T convert(T t);
    }


    ProcessControl withExceptionConverter(ExceptionConverter converter);

    void resetData(boolean cache);

    String prepareTerminalOpen(TerminalInitScriptConfig config,
                               FailableFunction<ShellControl, String, Exception> workingDirectory) throws Exception;

    void closeStdin() throws IOException;

    boolean isStdinClosed();

    boolean isRunning();

    ShellDialect getShellDialect();

    void writeLine(String line) throws IOException;

    void writeLine(String line, boolean log) throws IOException;

    void write(byte[] b) throws IOException;

    @Override
    void close() throws Exception;

    void kill();

    ProcessControl start() throws Exception;

    InputStream getStdout();

    OutputStream getStdin();

    InputStream getStderr();

    Charset getCharset();
}
