package io.xpipe.cli.util;

import io.xpipe.core.source.DataSourceId;
import picocli.CommandLine;

public class DataSourceIdConverter implements CommandLine.ITypeConverter<DataSourceId> {

    @Override
    public DataSourceId convert(String value) throws Exception {
        return DataSourceId.fromString(value);
    }
}
