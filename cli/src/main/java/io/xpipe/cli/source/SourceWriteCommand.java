package io.xpipe.cli.source;

import io.xpipe.beacon.exchange.cli.WriteExecuteExchange;
import io.xpipe.beacon.exchange.cli.WritePreparationExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.*;
import io.xpipe.core.impl.StdoutDataStore;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import picocli.CommandLine;

@CommandLine.Command(
        name = "write",
        header =
                "Writes the contents of a data source to a target destination, which can either be a file, stdout, another data store, or another data source.",
        description = "Sequentially outputs the data source contents to a destination. "
                + "The following output options are available:%n"
                + "- If a data store is specified through --output-store, the data is written to that data store with either the same type or a custom type passed through the --type option.%n"
                + "- If a data source name is specified through --output-source, the data is written to that data source.%n"
                + "- If no explicit output is specified, the data is written to stdout.%n"
                + "%n"
                + "In case additional configuration parameters are required to convert to the output type, "
                + "they can either be passed through -o options or can be set interactively.",
        sortOptions = false)
public class SourceWriteCommand extends BaseCommand {

    @CommandLine.Option(
            names = {"-t", "--type"},
            description = "The output format type",
            paramLabel = "<type>")
    String type;

    @CommandLine.Parameters(
            description = "The data source reference",
            arity = "0..1",
            paramLabel = "<source>",
            converter = DataSourceReferenceConverter.class)
    DataSourceReference ref = DataSourceReference.latest();

    @CommandLine.Option(
            names = {"--output-store"},
            description = "The output store to write to.",
            paramLabel = "<output store>",
            converter = DataStoreConverter.class)
    DataStore outputStore = StdoutDataStore.builder().build();

    @CommandLine.Option(
            names = {"--output-source"},
            description = "The output source to write to.",
            paramLabel = "<output source>",
            converter = DataSourceReferenceConverter.class)
    DataSourceReference outputSource;

    @CommandLine.Option(
            names = {"-m", "--mode"},
            description = "The write mode",
            paramLabel = "<mode>")
    String mode;

    @CommandLine.Mixin
    ConfigOverride config;

    @CommandLine.Mixin
    QuietOverride quietOverride;

    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        var req = WritePreparationExchange.Request.builder()
                .source(ref)
                .type(type)
                .outputStore(outputStore)
                .outputSource(outputSource)
                .build();
        WritePreparationExchange.Response response = con.performSimpleExchange(req);

        new DialogHandler(response.getConfig(), con).handle();

        var execReq = WriteExecuteExchange.Request.builder()
                .id(response.getConfig().getDialogId())
                .ref(ref)
                .mode(mode)
                .build();

        con.sendRequest(execReq);
        WriteExecuteExchange.Response res = con.receiveResponse();
        if (res.isHasBody()) {
            StreamDataStore stream = outputStore.asNeeded();
            try (var in = con.receiveBody();
                    var out = stream.openOutput()) {
                in.transferTo(out);
            }
        }
    }
}
