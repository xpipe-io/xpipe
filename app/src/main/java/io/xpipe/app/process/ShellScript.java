package io.xpipe.app.process;

import lombok.Value;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class ShellScript {

    String value;

    public static ShellScript empty() {
        return new ShellScript("");
    }

    public static ShellScript of(String s) {
        return s != null ? new ShellScript(s) : null;
    }

    public static ShellScript lines(String... lines) {
        return new ShellScript(Arrays.stream(lines).filter(s -> s != null).collect(Collectors.joining("\n")));
    }

    public static ShellScript lines(List<String> lines) {
        return new ShellScript(lines.stream().collect(Collectors.joining("\n")));
    }

    public String withoutShebang() {
        var shebang = value.startsWith("#!");
        if (shebang) {
            return value.lines().skip(1).collect(Collectors.joining("\n"));
        } else {
            return value;
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
