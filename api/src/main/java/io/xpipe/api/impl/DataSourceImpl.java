package io.xpipe.api.impl;

import io.xpipe.api.DataSource;
import io.xpipe.api.DataSourceConfig;
import io.xpipe.api.connector.XPipeConnection;
import io.xpipe.beacon.exchange.QueryDataSourceExchange;
import io.xpipe.beacon.exchange.ReadPreparationExchange;
import io.xpipe.beacon.exchange.StoreStreamExchange;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;

import java.io.InputStream;
import java.util.Map;

public abstract class DataSourceImpl implements DataSource {

    public static DataSource get(DataSourceReference ds) {
        return XPipeConnection.execute(con -> {
            var req = QueryDataSourceExchange.Request.builder().ref(ds).build();
            QueryDataSourceExchange.Response res = con.performSimpleExchange(req);
            var config = new DataSourceConfig(res.getProvider(), res.getConfig());
            switch (res.getInfo().getType()) {
                case TABLE -> {
                    var data = res.getInfo().asTable();
                    return new DataTableImpl(res.getId(), config, data);
                }
                case STRUCTURE -> {
                    var info = res.getInfo().asStructure();
                    return new DataStructureImpl(res.getId(), config, info);
                }
                case TEXT -> {
                    var info = res.getInfo().asText();
                    return new DataTextImpl(res.getId(), config, info);
                }
                case RAW -> {
                    var info = res.getInfo().asRaw();
                    return new DataRawImpl(res.getId(), config, info);
                }
            }
            throw new AssertionError();
        });
    }

    public static DataSource create(DataSourceId id, String type, Map<String,String> config, InputStream in) {
        var res = XPipeConnection.execute(con -> {
            var req = StoreStreamExchange.Request.builder().build();
            StoreStreamExchange.Response r = con.performOutputExchange(req, out -> in.transferTo(out));
            return r;
        });

        var store = res.getStore();

        var startReq = ReadPreparationExchange.Request.builder()
                .provider(type)
                .store(store)
                .build();
        var startRes = XPipeConnection.execute(con -> {
            ReadPreparationExchange.Response r = con.performSimpleExchange(startReq);
            return r;
        });

        var configInstance = startRes.getConfig();
        //TODO
//        configInstance.getConfigInstance().getCurrentValues().putAll(config);
//        var endReq = ReadExecuteExchange.Request.builder()
//                .target(id).dataStore(store).config(configInstance).build();
//        XPipeConnection.execute(con -> {
//            con.performSimpleExchange(endReq);
//        });
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
