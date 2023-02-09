package io.xpipe.cli.daemon;

import io.xpipe.beacon.BeaconConfig;
import io.xpipe.beacon.BeaconServer;
import io.xpipe.beacon.exchange.cli.StatusExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.InputArgumentConverter;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

import java.util.List;
import java.util.stream.Collectors;

@CommandLine.Command(name = "start", description = "Starts the X-Pipe daemon if it is not running")
public class StartCommand extends BaseCommand {

    @CommandLine.Mixin
    private HelpMixin help;

    @CommandLine.Parameters(
            description = "Optional arguments to pass to the daemon",
            paramLabel = "<input>",
            converter = InputArgumentConverter.class)
    List<String> inputs = List.of();

    @Override
    public Integer call() throws Exception {
        if (BeaconServer.isRunning()) {
            System.out.println("X-Pipe daemon is already running");
            return 0;
        }

        var arguments = inputs.stream()
                .map(s -> s.startsWith("\"") && s.endsWith("\"") ? s : "\"" + s + "\"")
                .collect(Collectors.joining(" "));
        System.setProperty(BeaconConfig.DAEMON_ARGUMENTS_PROP, arguments);

        return super.call();
    }

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        StatusExchange.Response res =
                con.performSimpleExchange(StatusExchange.Request.builder().build());
    }
}