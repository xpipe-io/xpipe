package io.xpipe.cli.meta;

import io.xpipe.beacon.exchange.cli.VersionExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.CliHelper;
import io.xpipe.cli.util.XPipeCliConnection;
import io.xpipe.core.util.XPipeInstallation;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "version",
        header = "Reports version information",
        description = "Reports version information of the X-Pipe installation.")
public class VersionCommand implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-e", "--extended"},
            description = "Query extended version information of the X-Pipe daemon. Requires the daemon to run.")
    public boolean extended;

    @Override
    public Integer call() throws Exception {
        if (extended) {
            return new BaseCommand() {
                @Override
                protected void execute(XPipeCliConnection con) throws Exception {
                    System.out.println(CommandLine.Help.Ansi.AUTO.string("\n@|bold X-Pipe Daemon|@"));
                    VersionExchange.Response r1 = con.performSimpleExchange(
                            VersionExchange.Request.builder().build());
                    System.out.println("  Version: " + r1.getVersion());
                    System.out.println("  Build: " + r1.getBuildVersion());
                    System.out.println("  JVM: " + r1.getJvmVersion());
                    System.out.println(CommandLine.Help.Ansi.AUTO.string("@|bold X-Pipe CLI|@"));
                    System.out.println("  JVM    : " + System.getProperty("java.vm.vendor") + " "
                            + System.getProperty("java.vm.name") + " ("
                            + System.getProperty("java.vm.version") + ")");
                }
            }.call();
        } else if (CliHelper.isProduction()) {
            var executable = CliHelper.findDaemonExecutable();
            if (!Files.exists(Path.of(executable))) {
                throw new IOException("X-Pipe installation not found at " + executable);
            }

            var version = XPipeInstallation.queryLocalInstallationVersion(executable);
            System.out.println(version);
        } else {
            System.out.println("?");
        }
        return 0;
    }
}
