package io.xpipe.api.impl;

import io.xpipe.api.DataSource;
import io.xpipe.api.DataTable;
import io.xpipe.api.XPipeApiConnector;
import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.ConnectorException;
import io.xpipe.beacon.ServerException;
import io.xpipe.beacon.exchange.ReadInfoExchange;
import io.xpipe.core.source.DataSourceId;

import java.net.URL;
import java.util.Map;

public abstract class DataSourceImpl implements DataSource {

    public static DataSource get(DataSourceId ds) {
        final DataSource[] source = new DataSource[1];
        new XPipeApiConnector() {
            @Override
            protected void handle(BeaconClient sc) throws ClientException, ServerException, ConnectorException {
                var req = ReadInfoExchange.Request.builder().sourceId(ds).build();
                ReadInfoExchange.Response res = performSimpleExchange(sc, req);
                switch (res.getType()) {
                    case TABLE -> {
                        var data = res.getTableData();
                        source[0] = new DataTableImpl(res.getSourceId(), data.getRowCount(), data.getDataType());
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

    public static DataSource wrap(URL url, String type, Map<String,String> config) {
        final DataSource[] source = new DataSource[1];
        new XPipeApiConnector() {
            @Override
            protected void handle(BeaconClient sc) throws ClientException, ServerException, ConnectorException {
                var req = ReadInfoExchange.Request.builder().sourceId(ds).build();
                ReadInfoExchange.Response res = performSimpleExchange(sc, req);
                switch (res.getType()) {
                    case TABLE -> {
                        var data = res.getTableData();
                        source[0] = new DataTableImpl(res.getSourceId(), data.getRowCount(), data.getDataType());
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

    private final DataSourceId sourceId;

    public DataSourceImpl(DataSourceId sourceId) {
        this.sourceId = sourceId;
    }

    @Override
    public DataSourceId getId() {
        return sourceId;
    }
}
