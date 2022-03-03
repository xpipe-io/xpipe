package io.xpipe.core.source;

import java.io.OutputStream;

public interface TextWriteConnection extends DataSourceConnection {

    OutputStream getOutputStream();
}
