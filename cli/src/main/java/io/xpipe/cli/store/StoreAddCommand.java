package io.xpipe.cli.store;

import io.xpipe.beacon.exchange.cli.StoreAddExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.CliHelper;
import io.xpipe.cli.util.DialogHandler;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import io.xpipe.core.impl.StdinDataStore;
import picocli.CommandLine;

@CommandLine.Command(
        name = "add",
        header =
                "Adds a new data store through an interactive dialog." + "%n%n"
                        + "The type of the data store has to be supplied at the command invocation. "
                        + "To obtain a list of possible data store types, use the xpipe store types command."
                        + "%n%n"
                        + "This command can also store piped input data by storing the data in the internal X-Pipe storage. "
                        + "In this case, the data store type has to be blank. "
                        + "Furthermore, the store name has to be specified with the name option as there is no interactivity in this piped case.",
        sortOptions = false)
public class StoreAddCommand extends BaseCommand {

    @CommandLine.Parameters(arity = "0..1", description = "The store type id.", paramLabel = "<type>")
    String type;
    @CommandLine.Option(
            arity = "0..1",
            names = {"-n", "--name"},
            description =
                    "The store name. If this option is not specified, you can also set the name interactively at the end of the "
                            + "data store creation process.",
            paramLabel = "<name>")
    String name;
    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        if (CliHelper.hasPipedInput()) {
            if (name == null) {
                throw new IllegalStateException("No store name specified");
            }

            if (type != null) {
                throw new IllegalStateException("Can't set explicit data store type for piped input");
            }

            var store = con.createInternalStreamStore(name);
            var stream = StdinDataStore.builder().build();
            try (var in = stream.openInput()) {
                con.writeStream(name, in);
            }
        } else {
            StoreAddExchange.Response res = con.performSimpleExchange(
                    StoreAddExchange.Request.builder().name(name).type(type).build());
            new DialogHandler(res.getConfig(), con).handle();
        }
    }
}
