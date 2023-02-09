package io.xpipe.cli.source;

import io.xpipe.beacon.exchange.cli.RemoveCollectionExchange;
import io.xpipe.beacon.exchange.cli.RemoveEntryExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.DataSourceReferenceConverter;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

@CommandLine.Command(
        name = "rm",
        aliases = {"remove", "delete", "del", "rem"},
        header = "Removes a data source or collection",
        description =
                "Removes a data source or collection." + "%n%n"
                        + "Note that this removal will only remove the data source within X-Pipe, not the underlying data store itself.",
        sortOptions = false)
public class SourceRemoveCommand extends BaseCommand {

    @CommandLine.Parameters(
            description = "The target to remove. This can either be a collection name or a data source reference.",
            paramLabel = "<target>",
            arity = "1")
    public String target;

    @CommandLine.Option(
            names = {"-c", "--collection"},
            description = "Indicates that the whole collection that is identified by the name should be removed")
    public boolean collection;

    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        if (collection) {
            var req = RemoveCollectionExchange.Request.builder()
                    .collectionName(target)
                    .build();
            con.performSimpleExchange(req);
            System.out.println("Successfully removed collection " + target);
        } else {
            var ref = new DataSourceReferenceConverter().convert(target);
            var req = RemoveEntryExchange.Request.builder().ref(ref).build();
            RemoveEntryExchange.Response res = con.performSimpleExchange(req);
            System.out.println("Successfully removed " + res.getId());
        }
    }
}
