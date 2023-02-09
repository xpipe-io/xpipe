package io.xpipe.cli.daemon;

import io.xpipe.beacon.exchange.cli.ModeExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.BusySpinner;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.ModeConverter;
import io.xpipe.cli.util.XPipeCliConnection;
import io.xpipe.core.util.XPipeDaemonMode;
import picocli.CommandLine;

@CommandLine.Command(name = "mode", description = "Switches the operation mode of the X-Pipe daemon")
public class ModeCommand extends BaseCommand {

    @CommandLine.Parameters(
            paramLabel = "<mode>",
            description =
                    "The mode to switch to. The mode id must be [background, tray, gui]. Note that not necessarily all operation "
                            + "modes are supported on your current platform.",
            converter = ModeConverter.class)
    XPipeDaemonMode mode;

    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        ModeExchange.Response res;
        try (var spinner = BusySpinner.start("Switching mode ...", false)) {
            res = con.performSimpleExchange(
                    ModeExchange.Request.builder().mode(mode).build());
        }
        System.out.println("Switched to mode " + highlight(res.getUsedMode().getDisplayName()));
    }
}