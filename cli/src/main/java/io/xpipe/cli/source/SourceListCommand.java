package io.xpipe.cli.source;

import io.xpipe.beacon.exchange.cli.ListCollectionsExchange;
import io.xpipe.beacon.exchange.cli.ListEntriesExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.PrettyTimeHelper;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

import java.time.Duration;

@CommandLine.Command(
        name = "list",
        header = "List data sources",
        description = "List all data sources contained in a given collection or the whole storage "
                + "if no collection name is passed.",
        sortOptions = false)
public class SourceListCommand extends BaseCommand {

    @CommandLine.Parameters(paramLabel = "<collection>", description = "The optional collection name", arity = "0..1")
    String collection;
    @CommandLine.Mixin
    private HelpMixin help;

    private void listCollections(XPipeCliConnection con) throws Exception {
        ListCollectionsExchange.Request req =
                ListCollectionsExchange.Request.builder().build();
        ListCollectionsExchange.Response rp = con.performSimpleExchange(req);

        if (rp.getEntries().size() == 0) {
            System.out.println("No collections found");
            return;
        }

        rp.getEntries().forEach(e -> {
            var name = e.getName() != null ? e.getName() : "Temporary";
            System.out.println(name + " (" + e.getSize() + ") "
                    + PrettyTimeHelper.get().format(e.getLastUsed().minus(Duration.ofSeconds(1))));
        });
    }

    private void listEntries(XPipeCliConnection con) throws Exception {
        ListEntriesExchange.Request req =
                ListEntriesExchange.Request.builder().collection(collection).build();
        ListEntriesExchange.Response rp = con.performSimpleExchange(req);

        if (rp.getEntries().size() == 0) {
            System.out.println("Collection does not contain any data sources");
            return;
        }

        System.out.println("Data sources");
        rp.getEntries().forEach(e -> {
            System.out.println(e.getName() + " (" + e.getType() + ") "
                    + PrettyTimeHelper.get().format(e.getLastUsed().minus(Duration.ofSeconds(1))));
        });
    }

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        if (collection == null) {
            listCollections(con);
        } else {
            listEntries(con);
        }
    }
}
