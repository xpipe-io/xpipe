package io.xpipe.cli.store;

import io.xpipe.beacon.exchange.cli.RenameStoreExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

@CommandLine.Command(
        name = "mv",
        aliases = {"rename", "move"},
        header = "Renames an existing data store. ",
        sortOptions = false)
public class StoreRenameCommand extends BaseCommand {

    @CommandLine.Parameters(paramLabel = "<store>", description = "The old store name.", arity = "1")
    String store;
    @CommandLine.Parameters(paramLabel = "<name>", description = "The new store name.", arity = "1")
    String name;
    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        RenameStoreExchange.Response res = con.performSimpleExchange(RenameStoreExchange.Request.builder()
                .storeName(store)
                .newName(name)
                .build());
        System.out.println("Store has been renamed to " + highlight(name));
    }
}
