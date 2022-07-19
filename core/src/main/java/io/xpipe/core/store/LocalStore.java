package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@JsonTypeName("local")
@Value
public class LocalStore implements ShellProcessStore {


    static class LocalProcessControl extends ProcessControl {

        private final InputStream input;
        private final ProcessBuilder builder;

        private Process process;

        LocalProcessControl(InputStream input, List<String> cmd) {
            this.input = input;
            var l = new ArrayList<String>();
            l.add("cmd");
            l.add("/c");
            l.addAll(cmd);
            builder = new ProcessBuilder(l);
        }

        @Override
        public void start() throws IOException {
            process = builder.start();

            var t = new Thread(() -> {
                try {
                    input.transferTo(process.getOutputStream());
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
            return process.waitFor();
        }

        @Override
        public InputStream getStdout() {
            return process.getInputStream();
        }

        @Override
        public InputStream getStderr() {
            return process.getErrorStream();
        }

        @Override
        public Charset getCharset() {
            return StandardCharsets.UTF_8;
        }
    }

    @Override
    public boolean exists(String file) {
        return Files.exists(Path.of(file));
    }

    @Override
    public String toDisplay() {
        return "local";
    }

    @Override
    public InputStream openInput(String file) throws Exception {
        var p = Path.of(file);
        return Files.newInputStream(p);
    }

    @Override
    public OutputStream openOutput(String file) throws Exception {
        var p = Path.of(file);
        return Files.newOutputStream(p);
    }

    @Override
    public ProcessControl prepareCommand(InputStream input, List<String> cmd) {
        return new LocalProcessControl(input, cmd);
    }

    @Override
    public ProcessControl preparePrivilegedCommand(InputStream input, List<String> cmd) throws Exception {
        return new LocalProcessControl(input, cmd);
    }

    @Override
    public ShellType determineType() {
        return ShellTypes.CMD;
    }
}
