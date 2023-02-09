package io.xpipe.cli.store;

import io.xpipe.beacon.exchange.WriteStreamExchange;
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
        name = "sink",
        header = "Creates an X-Pipe sink that blocks until a connection from a data consumer is made.",
        sortOptions = false)
public class StoreSinkCommand extends BaseCommand {

    @CommandLine.Option(
            names = {"-n", "--name"},
            description = "The custom sink name",
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
            name = "X-Pipe CLI Sink";
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

        var writeReq = WriteStreamExchange.Request.builder().name(name).build();
        con.performOutputExchange(writeReq, (out) -> {
            System.in.transferTo(out);
            out.flush();
        });
    }
}
