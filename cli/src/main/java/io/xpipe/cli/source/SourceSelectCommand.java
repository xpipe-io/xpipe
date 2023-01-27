package io.xpipe.cli.source;

import io.xpipe.beacon.exchange.cli.SelectExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.DataSourceReferenceConverter;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import io.xpipe.core.source.DataSourceReference;
import picocli.CommandLine;

@CommandLine.Command(
        name = "select",
        header = "Selects a default data source to be used in subsequent commands",
        description = "Selects a certain data source that will always be used as the "
                + "default in case a command does not explicitly specify another data source.")
public class SourceSelectCommand extends BaseCommand {

    @CommandLine.Parameters(
            description = "The data source reference",
            paramLabel = "<source>",
            arity = "1",
            converter = DataSourceReferenceConverter.class)
    DataSourceReference ref = DataSourceReference.latest();
    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        var req = SelectExchange.Request.builder().ref(ref).build();
        SelectExchange.Response res = con.performSimpleExchange(req);
        System.out.println("Selected " + ref.toString());
    }
}
