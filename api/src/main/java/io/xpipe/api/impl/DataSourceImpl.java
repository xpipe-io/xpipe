package io.xpipe.api.impl;

import io.xpipe.api.DataSource;
import io.xpipe.api.DataSourceConfig;
import io.xpipe.api.connector.XPipeConnection;
import io.xpipe.beacon.exchange.QueryDataSourceExchange;
import io.xpipe.beacon.exchange.ReadExchange;
import io.xpipe.beacon.exchange.StoreStreamExchange;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;

import java.io.InputStream;

public abstract class DataSourceImpl implements DataSource {

    public static DataSource get(DataSourceReference ds) {
        return XPipeConnection.execute(con -> {
            var req = QueryDataSourceExchange.Request.builder().ref(ds).build();
            QueryDataSourceExchange.Response res = con.performSimpleExchange(req);
            var config = new DataSourceConfig(res.getProvider(), res.getConfig());
            return switch (res.getInfo().getType()) {
                case TABLE -> {
                    var data = res.getInfo().asTable();
                    yield new DataTableImpl(res.getId(), config, data);
                }
                case STRUCTURE -> {
                    var info = res.getInfo().asStructure();
                    yield new DataStructureImpl(res.getId(), config, info);
                }
                case TEXT -> {
                    var info = res.getInfo().asText();
                    yield new DataTextImpl(res.getId(), config, info);
                }
                case RAW -> {
                    var info = res.getInfo().asRaw();
                    yield new DataRawImpl(res.getId(), config, info);
                }
                case COLLECTION -> throw new UnsupportedOperationException("Unimplemented case: " + res.getInfo().getType());
                default -> throw new IllegalArgumentException("Unexpected value: " + res.getInfo().getType());
            };
        });
    }

    public static DataSource create(DataSourceId id, String type, InputStream in) {
        var res = XPipeConnection.execute(con -> {
            var req = StoreStreamExchange.Request.builder().build();
            StoreStreamExchange.Response r = con.performOutputExchange(req, out -> in.transferTo(out));
            return r;
        });

        var store = res.getStore();

        var startReq = ReadExchange.Request.builder()
                .provider(type)
                .store(store)
                .target(id)
                .configureAll(false)
                .build();
        var startRes = XPipeConnection.execute(con -> {
            ReadExchange.Response r = con.performSimpleExchange(startReq);
            return r;
        });

        var configInstance = startRes.getConfig();
        XPipeConnection.finishDialog(configInstance);

        var ref = id != null ? DataSourceReference.id(id) : DataSourceReference.latest();
        return get(ref);
    }

    private final DataSourceId sourceId;
    private final DataSourceConfig config;

    public DataSourceImpl(DataSourceId sourceId, DataSourceConfig config) {
        this.sourceId = sourceId;
        this.config = config;
    }

    @Override
    public DataSourceId getId() {
        return sourceId;
    }

    @Override
    public DataSourceConfig getConfig() {
        return config;
    }
}
