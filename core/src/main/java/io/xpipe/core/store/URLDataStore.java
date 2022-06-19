package io.xpipe.core.store;

import lombok.Value;

import java.io.InputStream;
import java.net.URL;

@Value
public class URLDataStore implements StreamDataStore {

    URL url;

    @Override
    public InputStream openInput() throws Exception {
        return url.openStream();
    }

    @Override
    public boolean canOpen() {
        return true;
    }
}
