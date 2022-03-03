package io.xpipe.core.source;

import java.nio.charset.Charset;

public interface TextReadConnection extends DataSourceConnection {

    Charset getEncoding();
}
