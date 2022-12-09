package io.xpipe.core.process;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public interface ProcessControl extends Closeable, AutoCloseable {

     String prepareTerminalOpen() throws Exception;

    void closeStdin() throws IOException;

    boolean isStdinClosed();

    boolean isRunning();

    ShellType getShellType();

    void writeLine(String line) throws IOException;

    @Override
    void close() throws IOException;
    void kill() throws Exception;

    ProcessControl exitTimeout(Integer timeout);

    ProcessControl start() throws Exception;

    InputStream getStdout();

    OutputStream getStdin();

    InputStream getStderr();

    Charset getCharset();
}
