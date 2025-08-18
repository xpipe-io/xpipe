package io.xpipe.app.process;

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

    void refreshRunningState();

    void closeStdin() throws IOException;

    boolean isAnyStreamClosed();

    boolean isRunning(boolean refresh);

    ShellDialect getShellDialect();

    @Override
    void close() throws Exception;

    void shutdown() throws Exception;

    void kill();

    void killExternal();

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
