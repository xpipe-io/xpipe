package io.xpipe.cli.source;

import io.xpipe.beacon.exchange.cli.SourceProviderListExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

@CommandLine.Command(
        name = "types",
        header = "List all available data source types",
        description = "Lists all currently available data source types grouped into the main categories.",
        sortOptions = false)
public class SourceTypesCommand extends BaseCommand {

    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        var req = SourceProviderListExchange.Request.builder().build();
        SourceProviderListExchange.Response res = con.performSimpleExchange(req);
        res.getEntries().forEach((key, value) -> {
            System.out.println();
            System.out.println(header(key.name().toLowerCase()));
            for (var provider : value) {
                if (provider.isHidden()) {
                    continue;
                }

                System.out.println("  " + highlight(provider.getId()) + ": " + provider.getDescription());
            }
        });
    }
}
