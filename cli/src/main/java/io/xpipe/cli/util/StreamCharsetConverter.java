package io.xpipe.cli.util;

import io.xpipe.core.charsetter.StreamCharset;
import picocli.CommandLine;

public class StreamCharsetConverter implements CommandLine.ITypeConverter<StreamCharset> {

    @Override
    public StreamCharset convert(String value) throws Exception {
        return StreamCharset.get(value);
    }
}
