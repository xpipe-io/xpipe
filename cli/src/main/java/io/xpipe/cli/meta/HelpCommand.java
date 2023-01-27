package io.xpipe.cli.meta;

import picocli.CommandLine;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "help",
        header = "Displays help information about the specified command",
        helpCommand = true,
        description = {
            "%nWhen no subcommand is given, the usage help for the main command is displayed.",
            "If a subcommand is specified, the help for that command is shown.%n"
        })
public class HelpCommand implements CommandLine.IHelpCommandInitializable2, Callable<Integer> {

    private CommandLine self;
    private CommandLine.Help.ColorScheme colorScheme;

    @CommandLine.Parameters(
            paramLabel = "subcommand",
            arity = "1",
            description = "The subcommand to display the usage help message for.")
    private String command;

    @Override
    public Integer call() throws Exception {
        if (command == null) {
            throw new IllegalArgumentException("Missing subcommand");
        }

        CommandLine parent = self.getParent();
        CommandLine.Help.ColorScheme colors = colorScheme;

        Map<String, CommandLine> parentSubcommands = parent.getCommandSpec().subcommands();
        CommandLine subcommand = parentSubcommands.get(command);
        if (subcommand != null) {
            var outWriter = System.out;
            subcommand.usage(outWriter, colors);
        } else {
            throw new CommandLine.ParameterException(parent, "Unknown subcommand '" + command + "'.", null, command);
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void init(
            CommandLine helpCommandLine, CommandLine.Help.ColorScheme colorScheme, PrintWriter out, PrintWriter err) {
        this.self = helpCommandLine;
        this.colorScheme = colorScheme;
    }
}
