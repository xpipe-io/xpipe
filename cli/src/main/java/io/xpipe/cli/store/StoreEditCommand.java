package io.xpipe.cli.store;

import io.xpipe.beacon.exchange.cli.EditStoreExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.DialogHandler;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

@CommandLine.Command(
        name = "edit",
        header = "Edits an existing data store through an interactive dialog.",
        sortOptions = false)
public class StoreEditCommand extends BaseCommand {

    @CommandLine.Parameters(paramLabel = "<store>", description = "The name of the store to edit.", arity = "1")
    String store;

    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        EditStoreExchange.Response res = con.performSimpleExchange(
                EditStoreExchange.Request.builder().name(store).build());
        new DialogHandler(res.getDialog(), con).handle();
    }
}
