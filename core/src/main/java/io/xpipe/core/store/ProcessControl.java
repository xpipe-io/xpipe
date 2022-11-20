package io.xpipe.core.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public interface ProcessControl extends AutoCloseable {

    boolean isRunning();

    ShellType getShellType();

    void writeLine(String line) throws IOException;

    void typeLine(String line);

    @Override
    void close() throws IOException;
    void kill() throws Exception;

    ProcessControl exitTimeout(Integer timeout);

    ProcessControl start() throws Exception;

    boolean waitFor() throws Exception;

    InputStream getStdout();

    OutputStream getStdin();

    InputStream getStderr();

    Charset getCharset();
}
