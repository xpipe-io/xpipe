package io.xpipe.cli.daemon;

import io.xpipe.beacon.BeaconServer;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.StopExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.CliHelper;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

@CommandLine.Command(name = "stop", description = "Stops the X-Pipe daemon if it is running.")
public class StopCommand extends BaseCommand {

    @CommandLine.Mixin
    private HelpMixin help;

    @CommandLine.Option(
            names = {"-f", "--force"},
            description =
                    "Attempts to forcefully kill the running xpipe daemon process. Note that if this option is specified, the "
                            + "daemon process will be killed instantly and no attempt at a graceful shutdown is made.")
    private boolean force;

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
        if (force) {
            var file = CliHelper.findDaemonExecutable();
            if (file.isEmpty()) {
                throw new ClientException("Unable to locate daemon executable to kill");
            }

            var procs = ProcessHandle.allProcesses()
                    .filter(p -> p.info().command().orElse("").equals(file))
                    .toList();
            if (procs.size() > 0) {
                boolean success = procs.stream().allMatch(ProcessHandle::destroyForcibly);
                long pid = procs.get(0).pid();
                if (success) {
                    System.out.println("Successfully killed xpipe daemon process with pid " + pid);
                } else {
                    System.out.println("Failed to kill xpipe daemon process with pid " + pid);
                }
            } else {
                System.out.println("Found no running xpipe daemon process to kill");
            }
            return;
        }

        StopExchange.Response res =
                con.performSimpleExchange(StopExchange.Request.builder().build());
        if (res.isSuccess()) {
            System.out.println("Shutting down xpipe daemon");
        } else {
            System.out.println("Could not stop xpipe daemon");
        }
    }
}