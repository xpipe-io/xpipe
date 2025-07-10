package io.xpipe.app.process;

import lombok.Value;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class ShellScript {

    public static ShellScript lines(String... lines) {
        return new ShellScript(Arrays.stream(lines).collect(Collectors.joining("\n")));
    }

    public static ShellScript lines(List<String> lines) {
        return new ShellScript(lines.stream().collect(Collectors.joining("\n")));
    }

    String value;

    @Override
    public String toString() {
        return value;
    }
}
