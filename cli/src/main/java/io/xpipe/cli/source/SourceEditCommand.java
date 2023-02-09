package io.xpipe.cli.source;

import io.xpipe.beacon.exchange.EditExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.*;
import picocli.CommandLine;

@CommandLine.Command(
        name = "edit",
        header = "Edits an existing data source",
        description = "Changes the data source configuration of an existing data source. "
                + "The configuration parameters can either be changed interactively or by passing them through the -o/--option arguments. "
                + "By default, the command runs in interactive mode unless any --option argument is passed."
                + "%n%n"
                + "Only the existing configuration can be changed with the edit command, not the data source type itself. "
                + "To also change the data source type, use the xpipe convert command instead.",
        sortOptions = false)
public class SourceEditCommand extends BaseCommand {

    @CommandLine.Mixin
    SourceRefMixin source;

    @CommandLine.Mixin
    ConfigOverride config;

    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        var hasOverrides = ConfigOverride.hasOverrides();
        QuietOverride.setQuiet(hasOverrides);

        var startReq = EditExchange.Request.builder().ref(source.ref).build();
        EditExchange.Response response = con.performSimpleExchange(startReq);
        new DialogHandler(response.getConfig(), con).handle();
        System.out.println("Successfully applied the changes");
    }
}
