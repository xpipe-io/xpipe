package io.xpipe.core.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public interface ProcessControl extends AutoCloseable {

    UUID getUuid();

    ProcessControl withExceptionConverter(ExceptionConverter converter);

    void resetData();

    String prepareTerminalOpen(TerminalInitScriptConfig config, WorkingDirectoryFunction workingDirectory)
            throws Exception;

    void closeStdin() throws IOException;

    boolean isStdinClosed();

    boolean isRunning();

    ShellDialect getShellDialect();

    void writeLine(String line) throws IOException;

    void writeLine(String line, boolean log) throws IOException;

    void write(byte[] b) throws IOException;

    @Override
    void close() throws Exception;

    void shutdown() throws Exception;

    void kill();

    ProcessControl start() throws Exception;

    InputStream getStdout();

    OutputStream getStdin();

    InputStream getStderr();

    Charset getCharset();

    @FunctionalInterface
    interface ExceptionConverter {
        <T extends Throwable> T convert(T t);
    }
}
