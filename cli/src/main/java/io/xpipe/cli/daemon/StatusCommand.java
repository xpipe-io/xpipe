package io.xpipe.cli.daemon;

import io.xpipe.beacon.BeaconServer;
import io.xpipe.beacon.exchange.cli.StatusExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

@CommandLine.Command(name = "status", description = "Reports the current status of the X-Pipe daemon")
public class StatusCommand extends BaseCommand {

    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    public Integer call() throws Exception {
        if (!BeaconServer.isRunning()) {
            System.out.println("X-Pipe daemon is not running");
            return 0;
        }

        return super.call();
    }

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        StatusExchange.Response res =
                con.performSimpleExchange(StatusExchange.Request.builder().build());
        System.out.println("X-Pipe daemon is currently running in operation mode " + highlight(res.getMode()));
    }
}
