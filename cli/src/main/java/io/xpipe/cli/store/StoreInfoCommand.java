package io.xpipe.cli.store;

import io.xpipe.beacon.exchange.QueryStoreExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

import java.util.Map;

@CommandLine.Command(
        name = "info",
        header = "Displays information about the data store.",
        sortOptions = false)
public class StoreInfoCommand extends BaseCommand {

    @CommandLine.Parameters(paramLabel = "<store>", description = "The store name.", arity = "1")
    String store;

    @CommandLine.Mixin
    private HelpMixin help;

    private void printProperties(String store, String type, String information, String summary) {
        System.out.println();
        System.out.println("Data Store:");
        System.out.println("  name       : " + store);
        System.out.println("  type       : " + type);
        System.out.println("  information: " + information);
        System.out.println("  summary    : " + summary);
    }

    private void printConfig(Map<String, String> i) {
        if (i.size() == 0) {
            return;
        }

        System.out.println();
        System.out.println("Configuration parameters:");
        i.forEach((key, value) -> System.out.println("  " + key + "=" + value));
    }

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        var req = QueryStoreExchange.Request.builder().name(store).build();
        QueryStoreExchange.Response res = con.performSimpleExchange(req);

        printProperties(res.getName(), res.getProvider(), res.getInformation(), res.getSummary());
        printConfig(res.getConfig());
    }
}
