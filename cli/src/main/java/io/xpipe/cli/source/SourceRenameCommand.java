package io.xpipe.cli.source;

import io.xpipe.beacon.exchange.cli.RenameEntryExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.DataSourceIdConverter;
import io.xpipe.cli.util.DataSourceReferenceConverter;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import picocli.CommandLine;

@CommandLine.Command(
        name = "mv",
        aliases = {"rename", "move"},
        header = "Renames an existing data store. ",
        sortOptions = false)
public class SourceRenameCommand extends BaseCommand {

    @CommandLine.Parameters(
            paramLabel = "<source>",
            description = "The old source.",
            arity = "1",
            converter = DataSourceReferenceConverter.class)
    DataSourceReference old;
    @CommandLine.Parameters(
            paramLabel = "<new id>",
            description = "The new source id.",
            arity = "1",
            converter = DataSourceIdConverter.class)
    DataSourceId newId;
    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        RenameEntryExchange.Response res = con.performSimpleExchange(
                RenameEntryExchange.Request.builder().ref(old).newId(newId).build());
        System.out.println("Source has been moved to " + highlight(newId.toString()));
    }
}
