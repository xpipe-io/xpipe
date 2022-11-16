package io.xpipe.core.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public interface ProcessControl extends AutoCloseable {

    boolean isRunning();

    ShellType getShellType();

    String readResultLine(String input, boolean captureOutput) throws IOException;

    void writeLine(String line) throws IOException;

    void writeLine(String line, boolean captureOutput) throws IOException;

    void typeLine(String line);

    public default String readOutput() throws IOException {
        var id = UUID.randomUUID();
        writeLine("echo " + id, false);
        String lines = "";
        while (true) {
            var newLine = readLine();
            if (newLine.contains(id.toString())) {
                if (getShellType().echoesInput()) {
                    readLine();
                }

                break;
            }

            lines = lines + newLine + "\n";
        }
        return lines;
    }

    @Override
    void close() throws IOException;

    String readLine() throws IOException;

    void kill() throws IOException;

    ProcessControl exitTimeout(int timeout);

    ProcessControl start() throws Exception;

    boolean waitFor() throws Exception;

    InputStream getStdout();

    OutputStream getStdin();

    InputStream getStderr();

    Charset getCharset();
}
