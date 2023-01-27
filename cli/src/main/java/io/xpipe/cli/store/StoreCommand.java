package io.xpipe.cli.store;

import io.xpipe.cli.meta.HelpCommand;
import io.xpipe.cli.util.HelpMixin;
import picocli.CommandLine;

@CommandLine.Command(
        name = "store",
        aliases = {"connection", "str", "con"},
        header = "Commands for data store handling",
        description =
                "Various commands that work on data stores. To print help information about a specific sub command, use xpipe store"
                        + " help <subcommand>.",
        synopsisSubcommandLabel = "<subcommand>",
        subcommands = {
            HelpCommand.class,
            StoreAddCommand.class,
            StoreRenameCommand.class,
            StoreRemoveCommand.class,
            StoreListCommand.class,
            StoreEditCommand.class,
            StoreInfoCommand.class,
            StoreTypesCommand.class,
            StoreDrainCommand.class,
            StoreSinkCommand.class
        })
public class StoreCommand {

    @CommandLine.Mixin
    private HelpMixin help;
}
