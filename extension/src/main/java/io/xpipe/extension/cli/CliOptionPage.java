package io.xpipe.extension.cli;

import java.util.List;

public class CliOptionPage {

    private String description;
    private List<CliOption<?>> options;

    public CliOptionPage(String description, List<CliOption<?>> options) {
        this.description = description;
        this.options = options;
    }

    public String getDescription() {
        return description;
    }

    public List<CliOption<?>> getOptions() {
        return options;
    }
}
