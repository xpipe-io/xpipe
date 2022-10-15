package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.InputStream;
import java.net.URL;

@JsonTypeName("url")
@SuperBuilder
@Jacksonized
public class URLDataStore implements StreamDataStore {

    private final URL url;

    @Override
    public InputStream openInput() throws Exception {
        return url.openStream();
    }

    @Override
    public boolean canOpen() {
        return true;
    }
}
