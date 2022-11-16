package io.xpipe.api;

import java.io.InputStream;

public interface DataRaw extends DataSource {

    InputStream open();

    byte[] readAll();

    byte[] read(int maxBytes);
}
