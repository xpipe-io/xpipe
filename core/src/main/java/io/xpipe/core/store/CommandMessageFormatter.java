package io.xpipe.core.store;

import io.xpipe.core.process.CommandControl;

public interface CommandMessageFormatter {

    CommandMessageFormatter IDENTITY = (cc, message) -> message;

    String format(CommandControl cc, String message);

    default CommandMessageFormatter or(CommandMessageFormatter formatter) {
        return (sc, message) -> {
            var s = format(sc, message);
            if (!s.equals(message)) {
                return s;
            }

            return formatter.format(sc, s);
        };
    }
}
