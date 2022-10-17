package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.core.util.SecretValue;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@JsonTypeName("local")
public class LocalStore extends JacksonizedValue implements MachineFileStore, StandardShellStore {

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public boolean exists(String file) {
        return Files.exists(Path.of(file));
    }

    @Override
    public void mkdirs(String file) throws Exception {
        Files.createDirectories(Path.of(file).getParent());
    }

    @Override
    public NewLine getNewLine() {
        return ShellTypes.getDefault().getNewLine();
    }

    @Override
    public InputStream openInput(String file) throws Exception {
        var p = Path.of(file);
        return Files.newInputStream(p);
    }

    @Override
    public OutputStream openOutput(String file) throws Exception {
        mkdirs(file);
        var p = Path.of(file);
        return Files.newOutputStream(p);
    }

    @Override
    public ProcessControl prepareCommand(List<SecretValue> input, List<String> cmd, Integer timeout, Charset charset) {
        return new LocalProcessControl(input, cmd, getEffectiveTimeOut(timeout), charset);
    }

    @Override
    public ProcessControl preparePrivilegedCommand(List<SecretValue> input, List<String> cmd, Integer timeOut, Charset charset)
            throws Exception {
        return new LocalProcessControl(input, cmd, getEffectiveTimeOut(timeOut), charset);
    }

    @Override
    public ShellType determineType() throws Exception {
        return ShellTypes.getDefault();
    }

    class LocalProcessControl extends ProcessControl {

        private final List<SecretValue> input;
        private final Integer timeout;
        private final List<String> command;
        private final Charset charset;

        private Process process;

        LocalProcessControl(List<SecretValue> input, List<String> cmd, Integer timeout, Charset charset) {
            this.input = input;
            this.timeout = timeout;
            this.command = cmd;
            this.charset = charset;
        }

        private InputStream createInputStream() {
            var string =
                    input.stream().map(secret -> secret.getSecretValue()).collect(Collectors.joining("\n")) + "\r\n";
            return new ByteArrayInputStream(string.getBytes(StandardCharsets.US_ASCII));
        }

        @Override
        public void start() throws Exception {
            var type = LocalStore.this.determineType();
            var l = type.switchTo(command);
            var builder = new ProcessBuilder(l);
            process = builder.start();

            var t = new Thread(() -> {
                try (var inputStream = createInputStream()) {
                    process.getOutputStream().flush();
                    inputStream.transferTo(process.getOutputStream());
                    process.getOutputStream().close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            t.setDaemon(true);
            t.start();
        }

        @Override
        public int waitFor() throws Exception {
            if (timeout != null) {
                return process.waitFor(timeout, TimeUnit.SECONDS) ? 0 : -1;
            } else {
                return process.waitFor();
            }
        }

        @Override
        public InputStream getStdout() {
            return process.getInputStream();
        }

        @Override
        public OutputStream getStdin() {
            return process.getOutputStream();
        }

        @Override
        public InputStream getStderr() {
            return process.getErrorStream();
        }

        @Override
        public Charset getCharset() {
            return charset;
        }

        public Integer getTimeout() {
            return timeout;
        }
    }
}
