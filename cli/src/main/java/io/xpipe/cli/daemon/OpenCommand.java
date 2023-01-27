package io.xpipe.cli.daemon;

import io.xpipe.beacon.exchange.OpenExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.InputArgumentConverter;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "open", description = "Opens the specified inputs with the X-Pipe daemon", sortOptions = false)
public class OpenCommand extends BaseCommand {

    @CommandLine.Mixin
    private HelpMixin help;

    @CommandLine.Parameters(
            paramLabel = "<input>", converter = InputArgumentConverter.class)
    List<String> inputs = List.of();

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        con.performSimpleExchange(OpenExchange.Request.builder().arguments(inputs).build());
    }
}