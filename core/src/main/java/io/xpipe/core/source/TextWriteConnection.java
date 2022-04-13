package io.xpipe.core.source;

public interface TextWriteConnection extends DataSourceConnection {

    void writeLine(String line) throws Exception;
}
