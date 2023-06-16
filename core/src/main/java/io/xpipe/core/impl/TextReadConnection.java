package io.xpipe.core.impl;

import java.io.BufferedReader;
import java.util.stream.Stream;

public class TextReadConnection extends StreamReadConnection implements io.xpipe.core.source.TextReadConnection {

    private BufferedReader bufferedReader;

    public TextReadConnection(TextSource source) {
        super(source.getStore(), source.getCharset());
    }

    @Override
    public void init() throws Exception {
        super.init();
        bufferedReader = new BufferedReader(reader);
    }

    @Override
    public Stream<String> lines() {
        return bufferedReader.lines();
    }

    @Override
    public void close() throws Exception {
        bufferedReader.close();
    }
}
