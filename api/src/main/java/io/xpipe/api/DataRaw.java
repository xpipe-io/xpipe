package io.xpipe.api;

import io.xpipe.core.source.DataSourceInfo;

import java.io.InputStream;

public interface DataRaw extends DataSource {

    DataSourceInfo.Raw getInfo();

    InputStream open();

    byte[] readAll();

    byte[] read(int maxBytes);
}
