package io.xpipe.core.process;

public class CommandBuilder {

    public static CommandBuilder of() {
        return new CommandBuilder(false);
    }

    public static CommandBuilder ofNoQuotes() {
        return new CommandBuilder(true);
    }

    private CommandBuilder(boolean noQuoting) {
        this.noQuoting = noQuoting;
    }

    private final boolean noQuoting;
    private final StringBuilder builder = new StringBuilder();

    public CommandBuilder add(String s) {
        if (!builder.isEmpty()) {
            builder.append(' ');
        }

        if (s.contains(" ") || s.contains("\t")) {
            if (noQuoting) {
                throw new IllegalArgumentException("No quoting rule conflicts with spaces an argument");
            }

            s = "\"" + s + "\"";
        }

        builder.append(s);
        return this;
    }

    public String build() {
        return builder.toString();
    }
}
