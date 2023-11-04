package io.xpipe.core.process;

import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandBuilder {

    private final List<Element> elements = new ArrayList<>();

    private CommandBuilder() {}

    public static CommandBuilder of() {
        return new CommandBuilder();
    }

    public CommandBuilder addSeparator(String s) {
        elements.add(sc -> sc.getShellDialect().getConcatenationOperator());
        return this;
    }

    public CommandBuilder addIf(boolean b, String... s) {
        if (b) {
            for (String s1 : s) {
                elements.add(new Fixed(s1));
            }
        }
        return this;
    }

    public CommandBuilder add(String... s) {
        for (String s1 : s) {
            elements.add(new Fixed(s1));
        }
        return this;
    }

    public CommandBuilder remove(String s) {
        elements.removeIf(element -> element instanceof Fixed fixed && s.equals(fixed.string));
        return this;
    }

    public CommandBuilder addQuoted(String s) {
        elements.add(sc -> {
            if (s == null) {
                return null;
            }

            if (sc == null) {
                return "\"" + s + "\"";
            }

            return sc.getShellDialect().quoteArgument(s);
        });
        return this;
    }

    public CommandBuilder addSub(CommandBuilder sub) {
        elements.add(sc -> {
            if (sc == null) {
                return sub.buildSimple();
            }

            return sub.build(sc);
        });
        return this;
    }

    public CommandBuilder prepend(Element e) {
        elements.add(0, e);
        return this;
    }

    public CommandBuilder add(Element e) {
        elements.add(e);
        return this;
    }

    public CommandBuilder prepend(String... s) {
        elements.addAll(0, Arrays.stream(s).map(s2 -> new Fixed(s2)).toList());
        return this;
    }

    public CommandBuilder prependQuoted(String s) {
        return prepend("\"" + s + "\"");
    }

    public CommandBuilder addFile(String s) {
        elements.add(sc -> {
            if (s == null) {
                return null;
            }

            if (sc == null) {
                return "\"" + s + "\"";
            }

            return sc.getShellDialect().fileArgument(s);
        });
        return this;
    }

    public String build(ShellControl sc) throws Exception {
        List<String> list = new ArrayList<>();
        for (Element element : elements) {
            String evaluate = element.evaluate(sc);
            if (evaluate == null) {
                continue;
            }

            list.add(evaluate);
        }
        return String.join(" ", list);
    }

    public CommandControl buildCommand(ShellControl sc) {
        return sc.command(this);
    }

    @SneakyThrows
    public String buildSimple() {
        List<String> list = new ArrayList<>();
        for (Element element : elements) {
            String evaluate = element.evaluate(null);
            if (evaluate == null) {
                continue;
            }

            list.add(evaluate);
        }
        return String.join(" ", list);
    }

    public interface Element {

        String evaluate(ShellControl sc) throws Exception;
    }

    static class Fixed implements Element {

        private final String string;

        Fixed(String string) {
            this.string = string;
        }

        @Override
        public String evaluate(ShellControl sc) {
            return string;
        }
    }
}
