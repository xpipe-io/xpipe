package io.xpipe.core.store;

import java.util.Arrays;
import java.util.function.Function;

public interface MessageFormatter {

    static MessageFormatter conditional(String contains, Function<String, String> format) {
        return (message) -> message.contains(contains) ? format.apply(message) : message;
    }

    static String[] flatten(String[]... args) {
        return Arrays.stream(args).flatMap(strings -> Arrays.stream(strings)).toArray(String[]::new);
    }

    static MessageFormatter explanation(String explanation, String... contains) {
        return (message) -> Arrays.stream(contains).anyMatch(s->message.contains(s))? """
                %s
                ```
                %s
                ```
                """.formatted(explanation, message) : message;
    }

    static MessageFormatter explanation(String explanation) {
        return (message) -> """
                %s
                ```
                %s
                ```
                """.formatted(explanation, message);
    }


    static MessageFormatter suffix(String explanation, String... contains) {
        return (message) -> Arrays.stream(contains).anyMatch(s->message.contains(s))? """
                %s
                %s
                """.formatted(message, explanation) : message;
    }

    static MessageFormatter chain(MessageFormatter... formatters) {
        return message -> {
            for (MessageFormatter formatter : formatters) {
                message = formatter.format(message);
            }
            return message;
        };
    }


    static MessageFormatter or(MessageFormatter... formatters) {
        return (message) -> {
            for (MessageFormatter formatter : formatters) {
                var fmt = formatter.format(message);
                if (!fmt.equals(message)) {
                    return fmt;
                }

                message = fmt;
            }
            return message;
        };
    }

    String format(String message);

    default ShellMessageFormatter shell() {
        return (sc, message) -> MessageFormatter.this.format(message);
    }

    default CommandMessageFormatter command() {
        return (cc, message) -> MessageFormatter.this.format(message);
    }
}
