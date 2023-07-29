package io.xpipe.core.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandBuilder {

    public static CommandBuilder of() {
        return new CommandBuilder();
    }

    private CommandBuilder() {}

    private final List<Element> elements = new ArrayList<>();

    public static interface Element {

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

    public CommandBuilder addSeparator(String s) {
        elements.add(sc -> sc.getShellDialect().getConcatenationOperator());
        return this;
    }

    public CommandBuilder add(String... s) {
        for (String s1 : s) {
            elements.add(new Fixed(s1));
        }
        return this;
    }

    public CommandBuilder addQuoted(String s) {
        elements.add(new Fixed("\"" + s + "\""));
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
        elements.add(sc -> sc.getShellDialect().fileArgument(s));
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

    public String buildSimple() {
        return String.join(" ", elements.stream().map(element -> ((Fixed) element).string).toList());
    }
}
