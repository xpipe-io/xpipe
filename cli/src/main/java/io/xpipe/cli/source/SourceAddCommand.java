package io.xpipe.cli.source;

import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.ReadExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.*;
import io.xpipe.core.impl.InternalStreamStore;
import io.xpipe.core.impl.StdinDataStore;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import picocli.CommandLine;

@CommandLine.Command(
        name = "add",
        header = "Creates a data source from either a file, a data store, or data piped into stdin",
        description = "Reads the input, determines the appropriate data source type, and creates a data source from it."
                + "%n%n"
                + "The data source type is attempted to be automatically determined. "
                + "In case the automatic detection does not return the correct results or is not desired, "
                + "the input format can also be explicitly specified with the --type option."
                + "%n%n"
                + "To access the created data source later on, you can also specify a data source id with the --id option."
                + "This id consists out of a collection name and the actual data source name separated by a colon, e.g. mycollection:mysource"
                + "If no id is specified, an anonymous data source is created. It can still be used until another one is created."
                + "%n%n"
                + "In case the input format type requires additional configuration parameters, "
                + "they can either be passed through the -o/--option options or can be set interactively. "
                + "The -q/--quiet switch can be used to enforce non-interactivity.",
        sortOptions = false)
public class SourceAddCommand extends BaseCommand {

    // TODO
    @CommandLine.Option(
            names = {"--confirm"},
            description = "Interactively confirm all determined configuration parameters")
    public boolean confirm;
    @CommandLine.Option(
            names = {"-t", "--type"},
            description = "The data source type. Only needs to be explicitly specified in case "
                    + "the automatic detection does not return the correct results or is not desired.",
            paramLabel = "<type>")
    String type;
    @CommandLine.Option(
            names = {"-i", "--id"},
            description = "The canonical data source reference that can be used to access the contents later on.",
            paramLabel = "<id>",
            converter = DataSourceIdConverter.class)
    DataSourceId id = null;
    @CommandLine.Parameters(
            arity = "0..1",
            description =
                    "The input store. This can either be a data store name or a file name. If left empty, the stdin contents are used instead.",
            paramLabel = "<input>")
    String input;
    @CommandLine.Mixin
    ConfigOverride config;
    @CommandLine.Mixin
    QuietOverride quietOverride;
    @CommandLine.Mixin
    private HelpMixin help;

    private DataStore getStore(XPipeCliConnection con) throws Exception {
        DataStore store = null;
        if (input == null) {
            if (!CliHelper.hasPipedInput()) {
                throw new ClientException("No input specified");
            }

            store = StdinDataStore.builder().build();
        } else {
            store = new DataStoreConverter().convert(input);
        }

        if (store instanceof StreamDataStore stream) {
            if (stream.isContentExclusivelyAccessible()) {
                store = con.createInternalStreamStore();
                try (var in = stream.openInput()) {
                    con.writeStream((InternalStreamStore) store, in);
                }
            }
        }

        return store;
    }

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        if (confirm && (QuietOverride.get() || !CliHelper.canHaveUserInput())) {
            throw new ClientException("Can not confirm interactively in quiet mode");
        }

        DataStore store = getStore(con);
        var startReq = ReadExchange.Request.builder()
                .provider(type)
                .target(id)
                .store(store)
                .configureAll(confirm)
                .build();
        ReadExchange.Response response = con.performSimpleExchange(startReq);
        if (!new DialogHandler(response.getConfig(), con).handle()) {
            System.out.println("Data source creation canceled");
            return;
        }

        System.out.println("Successfully created data source"
                + (id != null ? CommandLine.Help.Ansi.AUTO.string(" with id @|bold,underline " + id + "|@") : ""));
    }
}
