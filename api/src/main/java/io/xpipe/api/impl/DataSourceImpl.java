package io.xpipe.api.impl;

import io.xpipe.api.DataSource;
import io.xpipe.api.XPipeApiConnector;
import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.ConnectorException;
import io.xpipe.beacon.ServerException;
import io.xpipe.beacon.exchange.InfoExchange;
import io.xpipe.beacon.exchange.StoreResourceExchange;
import io.xpipe.beacon.exchange.StoreStreamExchange;
import io.xpipe.core.source.DataSourceConfig;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

public abstract class DataSourceImpl implements DataSource {

    public static DataSource get(DataSourceReference ds) {
        final DataSource[] source = new DataSource[1];
        new XPipeApiConnector() {
            @Override
            protected void handle(BeaconClient sc) throws ClientException, ServerException, ConnectorException {
                var req = InfoExchange.Request.builder().ref(ds).build();
                InfoExchange.Response res = performSimpleExchange(sc, req);

            }
        }.execute();
        return source[0];
    }

    public static DataSource wrap(URL url, String type, Map<String,String> config) {
        final DataSource[] source = new DataSource[1];
        new XPipeApiConnector() {
            @Override
            protected void handle(BeaconClient sc) throws ClientException, ServerException, ConnectorException {
                var req = StoreResourceExchange.Request.builder()
                        .url(url).providerId(type).build();
                StoreResourceExchange.Response res = performOutputExchange(sc, req, out -> {
                    try (var s = url.openStream()) {
                        writeLength(sc, s.available());
                        s.transferTo(out);
                    }
                });
                switch (res.getInfo().getType()) {
                    case TABLE -> {
                        var data = res.getInfo().asTable();
                        source[0] = new DataTableImpl(res.getSourceId(), res.getConfig(), data);
                    }
                    case STRUCTURE -> {
                    }
                    case RAW -> {
                    }
                }
            }
        }.execute();
        return source[0];
    }

    public static DataSource wrap(InputStream in, String type, Map<String,String> config) {
        final DataSource[] source = new DataSource[1];
        new XPipeApiConnector() {
            @Override
            protected void handle(BeaconClient sc) throws ClientException, ServerException, ConnectorException {
                var req = StoreStreamExchange.Request.builder().type(type).build();
                StoreStreamExchange.Response res = performOutputExchange(sc, req, in::transferTo);

            }
        }.execute();
        return source[0];
    }

    private final DataSourceId sourceId;
    private final DataSourceConfig sourceConfig;

    public DataSourceImpl(DataSourceId sourceId, DataSourceConfig sourceConfig) {
        this.sourceId = sourceId;
        this.sourceConfig = sourceConfig;
    }

    @Override
    public DataSourceId getId() {
        return sourceId;
    }

    @Override
    public DataSourceConfig getConfig() {
        return sourceConfig;
    }
}
