package io.xpipe.core.store;

import io.xpipe.core.process.ShellControl;

import java.util.function.Function;

public interface ShellMessageFormatter {

    ShellMessageFormatter IDENTITY = (sc, message) -> message;

    static ShellMessageFormatter conditional(String contains, Function<String, String> format) {
        return (sc, message) -> message.contains(contains) ? format.apply(message) : message;
    }

    String format(ShellControl sc, String message);

    default ShellMessageFormatter or(ShellMessageFormatter formatter) {
        return (sc, message) -> {
            var s = format(sc, message);
            if (!s.equals(message)) {
                return s;
            }

            return formatter.format(sc, s);
        };
    }
}
