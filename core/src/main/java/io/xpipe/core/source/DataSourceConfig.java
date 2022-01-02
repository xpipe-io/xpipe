package io.xpipe.core.source;


import java.util.List;

public class DataSourceConfig {

    private String description;
    private List<Option<?>> options;

    public DataSourceConfig(String description, List<Option<?>> options) {
        this.description = description;
        this.options = options;
    }

    public String getDescription() {
        return description;
    }

    public List<Option<?>> getOptions() {
        return options;
    }

    public abstract static class Option<T> {

        private final String name;
        protected T value;

        public Option(String name) {
            this.name = name;
            this.value = null;
        }

        public Option(String name, T value) {
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
}
