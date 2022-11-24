package io.xpipe.core.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public interface ProcessControl extends AutoCloseable {

    static String join(List<String> command) {
        return command.stream().map(s -> s.contains(" ") ? "\"" + s + "\"" : s).collect(Collectors.joining(" "));
    }

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
