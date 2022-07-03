package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@JsonTypeName("local")
@Value
public class LocalStore implements ShellStore {

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
    public String executeAndRead(List<String> cmd) throws Exception {
        var p = prepare(cmd).redirectErrorStream(true);
        var proc = p.start();
        var b = proc.getInputStream().readAllBytes();
        proc.waitFor();
        //TODO
        return new String(b, StandardCharsets.UTF_16LE);
    }

    @Override
    public List<String> createCommand(List<String> cmd) {
        return cmd;
    }
}
