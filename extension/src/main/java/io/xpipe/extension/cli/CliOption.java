package io.xpipe.extension.cli;

public abstract class CliOption<T> {

    private final String name;
    protected T value;

    public CliOption(String name) {
        this.name = name;
        this.value = null;
    }

    public CliOption(String name, T value) {
        this.name = name;
        this.value = value;
    }

    protected abstract String enterValue(String val);

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }
}
