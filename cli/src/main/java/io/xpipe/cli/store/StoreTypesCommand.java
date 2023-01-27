package io.xpipe.cli.store;

import io.xpipe.beacon.exchange.cli.StoreProviderListExchange;
import io.xpipe.beacon.exchange.data.ProviderEntry;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

@CommandLine.Command(name = "types", header = "List all available data store type ids.")
public class StoreTypesCommand extends BaseCommand {

    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        var req = StoreProviderListExchange.Request.builder().build();
        StoreProviderListExchange.Response res = con.performSimpleExchange(req);
        res.getEntries().forEach((key, value) -> {
            System.out.println(key + ":");
            for (ProviderEntry providerEntry : value) {
                if (providerEntry.isHidden()) {
                    continue;
                }

                System.out.println("  " + highlight(providerEntry.getId()) + ": " + providerEntry.getDescription());
            }
        });
    }
}
