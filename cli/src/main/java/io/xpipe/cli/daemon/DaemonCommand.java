package io.xpipe.cli.daemon;

import io.xpipe.cli.meta.HelpCommand;
import io.xpipe.cli.util.HelpMixin;
import picocli.CommandLine;

@CommandLine.Command(
        name = "daemon",
        header = "Commands for controlling the X-Pipe daemon",
        synopsisSubcommandLabel = "<subcommand>",
        subcommands = {
            StartCommand.class,
            ModeCommand.class,
            StatusCommand.class,
            StopCommand.class,
            BeaconCommand.class,
            HelpCommand.class,
        })
public class DaemonCommand {

    @CommandLine.Mixin
    private HelpMixin help;
}
