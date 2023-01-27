package io.xpipe.cli.store;

import io.xpipe.beacon.exchange.cli.RemoveStoreExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

@CommandLine.Command(
        name = "rm",
        aliases = {"remove", "delete", "del", "rem"},
        header =
                "Removes an existing data store from the X-Pipe storage. Note that this does not mean that the underlying store is "
                        + "deleted as well.",
        sortOptions = false)
public class StoreRemoveCommand extends BaseCommand {

    @CommandLine.Parameters(paramLabel = "<store>", description = "The store name.", arity = "1")
    String store;
    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        con.performSimpleExchange(
                RemoveStoreExchange.Request.builder().storeName(store).build());
        System.out.println("Store " + highlight(store) + " has been removed");
    }
}
