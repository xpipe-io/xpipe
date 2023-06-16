package io.xpipe.core.source;

import java.io.InputStream;

public interface CollectionWriteConnection extends DataSourceConnection {

    void write(String entry, InputStream content);
}
