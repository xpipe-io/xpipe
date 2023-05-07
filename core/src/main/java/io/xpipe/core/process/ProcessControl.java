package io.xpipe.core.process;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;

public interface ProcessControl extends Closeable, AutoCloseable {

    void resetData();

    ExecutorService getStdoutReader();

    ExecutorService getStderrReader();

    ProcessControl sensitive();

    String prepareTerminalOpen(String displayName) throws Exception;

    void closeStdin() throws IOException;

    boolean isStdinClosed();

    boolean isRunning();

    ShellDialect getShellDialect();

    void writeLine(String line) throws IOException;

    void write(byte[] b) throws IOException;

    @Override
    void close() throws IOException;

    void kill() throws Exception;

    ProcessControl start() throws Exception;

    InputStream getStdout();

    OutputStream getStdin();

    InputStream getStderr();

    Charset getCharset();
}
