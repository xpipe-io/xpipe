package io.xpipe.cli.store;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.asciithemes.a7.A7_Grids;
import io.xpipe.beacon.exchange.cli.ListStoresExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.HelpMixin;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(
        name = "ls",
        aliases = {"list"},
        header = "List all saved data stores.")
public class StoreListCommand extends BaseCommand {

    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        ListStoresExchange.Request req = ListStoresExchange.Request.builder().build();
        ListStoresExchange.Response rp = con.performSimpleExchange(req);

        if (rp.getEntries().size() == 0) {
            System.out.println("No stores found");
            return;
        }

        AsciiTable at = new AsciiTable();
        at.addRow(List.of("Name", "Type ID"));
        at.addRule();
        rp.getEntries().forEach(e -> {
            at.addRow(e.getName(), e.getType());
        });
        at.getContext().setGrid(A7_Grids.minusBarPlusEquals());
        at.getRenderer().setCWC(new CWC_LongestLine());
        at.setPaddingLeft(1);
        at.setPaddingRight(1);
        System.out.println(at.render());
    }
}
