package io.xpipe.cli.daemon;

import io.xpipe.beacon.BeaconFormat;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

import java.io.IOException;
import java.net.SocketException;

@CommandLine.Command(name = "beacon", description = "Uses the input and output to simulate a X-Pipe beacon connection")
public class BeaconCommand extends BaseCommand {

    @CommandLine.Mixin
    private HelpMixin help;

    @CommandLine.Option(
            names = {"-r", "--raw"},
            description =
                    "Writes the input and output from and to the socket completely raw. Only useful for programmatic use.")
    private boolean raw;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        var client = con.getBeaconClient();

        var input = raw ? client.getRawInputStream() : BeaconFormat.readBlocks(client.getRawInputStream());
        var output = raw ? client.getRawOutputStream() : BeaconFormat.writeBlocks(client.getRawOutputStream());

        var systemOut = new Thread(() -> {
            while (true) {
                try {
                    var read = input.read();
                    if (read == -1) {
                        break;
                    } else {
                        System.out.write(read);
                    }
                } catch (SocketException ex) {
                    break;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    break;
                }
            }
            System.out.close();
        });
        systemOut.setDaemon(true);
        systemOut.start();

        var systemIn = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = System.in.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }

                output.flush();
                if (!raw) {
                    output.close();
                }
            } catch (SocketException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        systemIn.setDaemon(true);
        systemIn.start();

        systemOut.join();
        systemIn.join();
    }
}
