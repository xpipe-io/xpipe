package io.xpipe.cli.util;

import io.xpipe.core.source.DataSourceReference;
import picocli.CommandLine;

public class DataSourceReferenceConverter implements CommandLine.ITypeConverter<DataSourceReference> {

    @Override
    public DataSourceReference convert(String value) throws Exception {
        return DataSourceReference.parse(value);
    }
}
