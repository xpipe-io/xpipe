package io.xpipe.cli.source;

import io.xpipe.beacon.exchange.QueryDataSourceExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.SourceRefMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import io.xpipe.core.source.DataSourceId;
import picocli.CommandLine;

import java.util.Map;

@CommandLine.Command(name = "info", header = "Prints information about a stored data source", sortOptions = false)
public class SourceInfoCommand extends BaseCommand {

    @CommandLine.Mixin
    SourceRefMixin source;

    @CommandLine.Mixin
    private HelpMixin help;

    private void printProperties(DataSourceId id, String store) {
        System.out.println();
        System.out.println("Data source:");
        System.out.println("  id       : " + (id != null ? id : "<anonymous>"));
        System.out.println("  store    : " + store);
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
        var req = QueryDataSourceExchange.Request.builder().ref(source.ref).build();
        QueryDataSourceExchange.Response res = con.performSimpleExchange(req);

        printProperties(res.getId(), res.getInformation());
        printConfig(res.getConfig());
    }
}
