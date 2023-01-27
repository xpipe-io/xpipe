package io.xpipe.cli.util;

import picocli.CommandLine;

public class HelpMixin {

    @CommandLine.Option(
            names = {"${picocli.help.name.0:--h}", "${picocli.help.name.1:---help}"},
            usageHelp = true,
            descriptionKey = "mixinStandardHelpOptions.help",
            description = "Show this help message and exit.",
            hidden = true)
    private boolean helpRequested;
}
