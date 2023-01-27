package io.xpipe.cli.source;

import io.xpipe.cli.meta.HelpCommand;
import io.xpipe.cli.util.HelpMixin;
import picocli.CommandLine;

@CommandLine.Command(
        name = "source",
        aliases = {"src"},
        header = "Commands for data source handling",
        description =
                "Various commands that work on data sources. To obtain help information about a specific sub command, use xpipe source"
                        + " help <subcommand>.",
        synopsisSubcommandLabel = "<subcommand>",
        subcommands = {
            SourceAddCommand.class,
            SourceWriteCommand.class,
            SourceInfoCommand.class,
            SourceEditCommand.class,
            SourcePeekCommand.class,
            SourceConvertCommand.class,
            SourceSelectCommand.class,
            SourceRenameCommand.class,
            SourceRemoveCommand.class,
            SourceListCommand.class,
            SourceTypesCommand.class,
            HelpCommand.class
        })
public class SourceCommand {
    @CommandLine.Mixin
    private HelpMixin help;
}
