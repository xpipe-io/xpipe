package io.xpipe.cli.source;

import io.xpipe.beacon.exchange.cli.ConvertExchange;
import io.xpipe.cli.BaseCommand;
import io.xpipe.cli.util.*;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceType;
import picocli.CommandLine;

@CommandLine.Command(
        name = "convert",
        header = "Converts a data source to a different type",
        description =
                "The conversion is taking place on the data source view level, not the actual content itself. "
                        + "It can therefore be seen as a reinterpretation of existing data rather than an actual conversion."
                        + "%n%n"
                        + "The new data source type can either be specified explicitly with the --type option or in a more "
                        + "implicit way by passing a general category with the --category option. "
                        + "When the explicit type is specified, the conversion is performed forcefully. In case only the category is specified, "
                        + "it will check whether the current type of the data source supports conversion to that new category."
                        + "%n%n"
                        + "The data source can either be converted in-place or out of place by creating a new converted copy with the --new option.",
        sortOptions = false)
public class SourceConvertCommand extends BaseCommand {

    @CommandLine.Option(
            names = {"-c", "--category"},
            description =
                    "The general category to use for conversion instead of an explicit type. Valid values: ${COMPLETION-CANDIDATES}")
    public Category category;
    @CommandLine.Mixin
    SourceRefMixin source;
    @CommandLine.Option(
            names = {"-t", "--type"},
            description = "The data source type to convert to",
            paramLabel = "<type>")
    String type;
    @CommandLine.Option(
            names = {"-n", "--new"},
            description = "The data source id of a newly created copy",
            paramLabel = "<source id>",
            converter = DataSourceIdConverter.class)
    DataSourceId copyTarget;
    @CommandLine.Mixin
    ConfigOverride config;
    @CommandLine.Mixin
    QuietOverride fixed;
    @CommandLine.Mixin
    private HelpMixin help;

    @Override
    protected void execute(XPipeCliConnection con) throws Exception {
        var startReq = ConvertExchange.Request.builder()
                .newProvider(type)
                .newCategory(category != null ? category.getWrapped() : null)
                .ref(source.ref)
                .copyId(copyTarget)
                .build();
        ConvertExchange.Response response = con.performSimpleExchange(startReq);

        if (response.getConfig() == null) {
            return;
        }

        var config = response.getConfig();
        new DialogHandler(config, con).handle();
    }

    public static enum Category {
        table(DataSourceType.TABLE),
        raw(DataSourceType.RAW),
        text(DataSourceType.TEXT),
        collection(DataSourceType.COLLECTION),
        structure(DataSourceType.STRUCTURE);

        private final DataSourceType wrapped;

        Category(DataSourceType wrapped) {
            this.wrapped = wrapped;
        }

        public DataSourceType getWrapped() {
            return wrapped;
        }
    }
}
