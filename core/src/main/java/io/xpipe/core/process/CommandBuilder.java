package io.xpipe.core.process;

import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.FailableConsumer;
import io.xpipe.core.util.FailableFunction;

import lombok.Getter;
import lombok.SneakyThrows;

import java.util.*;
import java.util.function.Function;

public class CommandBuilder {

    private final List<Element> elements = new ArrayList<>();

    @Getter
    private final Map<String, Element> environmentVariables = new LinkedHashMap<>();

    private final List<FailableConsumer<ShellControl, Exception>> setup = new ArrayList<>();

    @Getter
    private CountDown countDown;

    @Getter
    private UUID uuid;

    private CommandBuilder() {}

    public static CommandBuilder of() {
        return new CommandBuilder();
    }

    public static CommandBuilder ofString(String s) {
        return new CommandBuilder().add(s);
    }

    public static CommandBuilder ofFunction(FailableFunction<ShellControl, String, Exception> command) {
        return CommandBuilder.of().add(sc -> command.apply(sc));
    }

    public CommandBuilder setup(FailableConsumer<ShellControl, Exception> consumer) {
        setup.add(consumer);
        return this;
    }

    public CommandBuilder fixedEnvrironment(String k, String v) {
        environmentVariables.put(k, new Fixed(v));
        return this;
    }

    public CommandBuilder envrironment(String k, Element v) {
        environmentVariables.put(k, v);
        return this;
    }

    public CommandBuilder fixedEnvrironment(Map<String, String> map) {
        map.forEach((s, s2) -> fixedEnvrironment(s, s2));
        return this;
    }

    public CommandBuilder envrironment(Map<String, Element> map) {
        environmentVariables.putAll(map);
        return this;
    }

    public CommandBuilder discardOutput() {
        elements.add(sc -> sc.getShellDialect().getDiscardOperator());
        return this;
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

    public CommandBuilder add(int index, String... s) {
        for (String s1 : s) {
            elements.add(index++, new Fixed(s1));
        }
        return this;
    }

    public CommandBuilder add(int index, Element... s) {
        for (var s1 : s) {
            elements.add(index++, s1);
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

    public CommandBuilder addQuoted(int index, String s) {
        elements.add(index, sc -> {
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

    public CommandBuilder add(CommandBuilder sub) {
        elements.addAll(sub.elements);
        environmentVariables.putAll(sub.environmentVariables);
        return this;
    }

    public CommandBuilder prepend(Element e) {
        elements.addFirst(e);
        return this;
    }

    public CommandBuilder add(Element e) {
        elements.add(e);
        return this;
    }

    public CommandBuilder addAll(List<String> s) {
        for (String s1 : s) {
            elements.add(new Fixed(s1));
        }
        return this;
    }

    public CommandBuilder addAll(FailableFunction<ShellControl, List<String>, Exception> f) {
        elements.add(sc -> String.join(" ", f.apply(sc)));
        return this;
    }

    public CommandBuilder prepend(String... s) {
        elements.addAll(0, Arrays.stream(s).map(s2 -> new Fixed(s2)).toList());
        return this;
    }

    public CommandBuilder prependQuoted(String s) {
        return prepend("\"" + s + "\"");
    }

    public CommandBuilder addFile(FailableFunction<ShellControl, String, Exception> f) {
        elements.add(sc -> {
            if (f == null) {
                return null;
            }

            if (sc == null) {
                return "\"" + f.apply(null) + "\"";
            }

            return sc.getShellDialect().fileArgument(f.apply(sc));
        });
        return this;
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

    public CommandBuilder addFile(FilePath s) {
        return addFile(s.toString());
    }

    public CommandBuilder addLiteral(String s) {
        elements.add(sc -> {
            if (s == null) {
                return null;
            }

            if (sc == null) {
                return "\"" + s + "\"";
            }

            return sc.getShellDialect().literalArgument(s);
        });
        return this;
    }

    public CommandBuilder addFiles(SequencedCollection<String> s) {
        s.forEach(this::addFile);
        return this;
    }

    public String buildBase(ShellControl sc) throws Exception {
        return String.join(" ", buildBaseParts(sc));
    }

    public List<String> buildBaseParts(ShellControl sc) throws Exception {
        countDown = CountDown.of();
        uuid = UUID.randomUUID();

        for (FailableConsumer<ShellControl, Exception> s : setup) {
            s.accept(sc);
        }

        List<String> list = new ArrayList<>();
        for (Element element : elements) {
            String evaluate = element.evaluate(sc);
            if (evaluate == null) {
                continue;
            }

            list.add(evaluate);
        }
        return list;
    }

    public Map<String, String> buildEnvironmentVariables(ShellControl sc) throws Exception {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (var e : environmentVariables.entrySet()) {
            var v = e.getValue().evaluate(sc);
            if (v != null) {
                map.put(e.getKey(), v);
            }
        }
        return map;
    }

    public String buildFull(ShellControl sc) throws Exception {
        if (sc == null) {
            return buildSimple();
        }

        var s = buildBase(sc);
        Map<String, String> map = buildEnvironmentVariables(sc);
        return sc.getShellDialect().assembleCommand(s, map);
    }

    public CommandControl build(ShellControl sc) {
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
