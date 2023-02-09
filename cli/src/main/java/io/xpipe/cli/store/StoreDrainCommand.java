package io.xpipe.cli.store;

import io.xpipe.beacon.exchange.ReadStreamExchange;
import io.xpipe.beacon.exchange.cli.StoreAddExchange;
import io.xpipe.beacon.util.QuietDialogHandler;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.StreamCharsetConverter;
import io.xpipe.cli.util.XPipeCliConnection;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.impl.SinkDrainStore;
import picocli.CommandLine;

@CommandLine.Command(
        name = "drain",
        header = "Creates an X-Pipe drain that blocks until a connection from a data producer is made.",
        sortOptions = false)
public class StoreDrainCommand extends BaseCommand {

    @CommandLine.Option(
            names = {"-n", "--name"},
            description = "The custom drain name",
            paramLabel = "<name>")
    String name;

    @CommandLine.Option(
            names = {"--encoding"},
            description = "A custom encoding to use instead of the default encoding of the console",
            converter = StreamCharsetConverter.class,
            paramLabel = "<encoding>")
    StreamCharset encoding;

    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        if (name == null) {
            name = "X-Pipe CLI Drain";
        }

        var encodingToUse = encoding;
        if (encodingToUse == null) {
            var console = System.console();
            encodingToUse = console != null ? StreamCharset.get(console.charset(), false) : StreamCharset.UTF8;
        }

        var store = SinkDrainStore.builder()
                .charset(encodingToUse)
                .newLine(NewLine.platform())
                .build();

        var addReq =
                StoreAddExchange.Request.builder().storeInput(store).name(name).build();
        StoreAddExchange.Response addRes = con.performSimpleExchange(addReq);
        QuietDialogHandler.handle(addRes.getConfig(), con);

        var readReq = ReadStreamExchange.Request.builder().name(name).build();
        con.performInputExchange(readReq, (drainResponse, in) -> {
            in.transferTo(System.out);
            System.out.flush();
        });
    }
}
