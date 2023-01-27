package io.xpipe.cli.util;

import io.xpipe.core.source.DataSourceReference;
import picocli.CommandLine;

public class SourceRefMixin {

    @CommandLine.Parameters(
            description =
                    """
            The data source reference. A data source reference supports multiple different formats:
            - If not specified, the latest data source will automatically be used
            - If a data source name is specified, the matching data source will be selected
            - In case there are multiple data sources with the same name, a fully qualified data source id can be used
            """,
            paramLabel = "<source>",
            arity = "0..1",
            converter = DataSourceReferenceConverter.class)
    public DataSourceReference ref = DataSourceReference.latest();
}
