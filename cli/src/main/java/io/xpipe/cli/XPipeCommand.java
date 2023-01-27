package io.xpipe.cli;

import io.xpipe.cli.daemon.DaemonCommand;
import io.xpipe.cli.daemon.OpenCommand;
import io.xpipe.cli.meta.HelpCommand;
import io.xpipe.cli.meta.VersionCommand;
import io.xpipe.cli.source.SourceCommand;
import io.xpipe.cli.store.StoreCommand;
import io.xpipe.cli.util.CliHelper;
import io.xpipe.cli.util.HelpMixin;
import picocli.CommandLine;

@CommandLine.Command(
        name = "xpipe",
        header = "Command-line interface for X-Pipe.",
        description =
                "Command-line interface to interact with the X-Pipe daemon." + "%n%n"
                        + "For a full reference, see either the man pages (local or online) or the help command, available via xpipe help <subcommand>.",
        synopsisSubcommandLabel = "<subcommand>",
        exitCodeListHeading = "%nExit Codes:%n",
        exitCodeList = {
            "0:Successful execution",
            "1:Client error: one or more inputs were not correctly specified",
            "2:Internal error: An internal error occurred in the X-Pipe daemon while executing the command",
            "3:Connection error: could either not start or communicate to the X-Pipe daemon"
        },
        subcommands = {
                OpenCommand.class,
            StoreCommand.class,
            SourceCommand.class,
            DaemonCommand.class,
            HelpCommand.class,
            VersionCommand.class
        },
        sortOptions = false)
public class XPipeCommand {

    private static final CommandLine commandLine;

    static {
        var cmd = new CommandLine(new XPipeCommand());
        cmd.setUsageHelpAutoWidth(true);
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        cmd.setOptionsCaseInsensitive(true);
        cmd.setSubcommandsCaseInsensitive(true);
        commandLine = cmd;
    }

    @CommandLine.Mixin
    private HelpMixin help;

    public static int execute(String... args) throws Exception {
        try {
            commandLine.parseArgs(args);
            return commandLine.execute(args);
        } catch (Exception ex) {
            if (CliHelper.isProduction()) {
                if (CliHelper.shouldPrintStackTrace()) {
                    ex.printStackTrace();
                } else {
                    System.err.println(ex.getMessage());
                }
            } else {
                ex.printStackTrace();
            }
            return 1;
        }
    }
}
