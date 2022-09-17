package io.xpipe.core.impl;

import io.xpipe.core.source.StreamWriteConnection;

public class TextWriteConnection extends StreamWriteConnection implements io.xpipe.core.source.TextWriteConnection {

    private final TextSource source;

    public TextWriteConnection(TextSource source) {
        super(source.getStore(), source.getCharset());
        this.source = source;
    }

    @Override
    public void writeLine(String line) throws Exception {
        writer.write(line);
        writer.write(source.getNewLine().getNewLine());
    }
}
