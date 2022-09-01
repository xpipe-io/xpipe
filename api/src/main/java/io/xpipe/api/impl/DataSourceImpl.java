package io.xpipe.api.impl;

import io.xpipe.api.DataSource;
import io.xpipe.api.DataSourceConfig;
import io.xpipe.api.connector.XPipeConnection;
import io.xpipe.beacon.exchange.*;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;

import java.io.InputStream;

public abstract class DataSourceImpl implements DataSource {

    @Override
    public void forwardTo(DataSource target) {
        XPipeConnection.execute(con -> {
            var req = ForwardExchange.Request.builder()
                    .source(DataSourceReference.id(sourceId))
                    .target(DataSourceReference.id(target.getId()))
                    .build();
            ForwardExchange.Response res = con.performSimpleExchange(req);
        });
    }

    @Override
    public void appendTo(DataSource target) {
        XPipeConnection.execute(con -> {
            var req = ForwardExchange.Request.builder()
                    .source(DataSourceReference.id(sourceId))
                    .target(DataSourceReference.id(target.getId()))
                    .append(true)
                    .build();
            ForwardExchange.Response res = con.performSimpleExchange(req);
        });

    }

    public static DataSource get(DataSourceReference ds) {
        return XPipeConnection.execute(con -> {
            var req = QueryDataSourceExchange.Request.builder().ref(ds).build();
            QueryDataSourceExchange.Response res = con.performSimpleExchange(req);
            var config = new DataSourceConfig(res.getProvider(), res.getConfig());
            return switch (res.getInfo().getType()) {
                case TABLE -> {
                    var data = res.getInfo().asTable();
                    yield new DataTableImpl(res.getId(), config, data, res.getInternalSource());
                }
                case STRUCTURE -> {
                    var info = res.getInfo().asStructure();
                    yield new DataStructureImpl(res.getId(), config, info, res.getInternalSource());
                }
                case TEXT -> {
                    var info = res.getInfo().asText();
                    yield new DataTextImpl(res.getId(), config, info, res.getInternalSource());
                }
                case RAW -> {
                    var info = res.getInfo().asRaw();
                    yield new DataRawImpl(res.getId(), config, info, res.getInternalSource());
                }
                case COLLECTION -> throw new UnsupportedOperationException("Unimplemented case: " + res.getInfo().getType());
                default -> throw new IllegalArgumentException("Unexpected value: " + res.getInfo().getType());
            };
        });
    }

    public static DataSource create(DataSourceId id, io.xpipe.core.source.DataSource<?> source) {
        var startReq = AddSourceExchange.Request.builder()
                .source(source)
                .target(id)
                .build();
        var returnedId = XPipeConnection.execute(con -> {
            AddSourceExchange.Response r = con.performSimpleExchange(startReq);
            return r.getId();
        });

        var ref = DataSourceReference.id(returnedId);
        return get(ref);
    }

    public static DataSource create(DataSourceId id, String type, DataStore store) {
        if (store instanceof StreamDataStore s && s.isLocalToApplication()) {
            var res = XPipeConnection.execute(con -> {
                var req = StoreStreamExchange.Request.builder().build();
                StoreStreamExchange.Response r = con.performOutputExchange(req, out -> {
                    try (InputStream inputStream = s.openInput()) {
                        inputStream.transferTo(out);
                    }
                });
                return r;
            });

            store = res.getStore();
        }

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

        var ref = id != null ?
                DataSourceReference.id(id) :
                DataSourceReference.latest();
        return get(ref);
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

        var ref = id != null ?
                DataSourceReference.id(id) :
                DataSourceReference.latest();
        return get(ref);
    }

    private final DataSourceId sourceId;
    private final DataSourceConfig config;
    private final io.xpipe.core.source.DataSource<?> internalSource;

    public DataSourceImpl(DataSourceId sourceId, DataSourceConfig config, io.xpipe.core.source.DataSource<?> internalSource) {
        this.sourceId = sourceId;
        this.config = config;
        this.internalSource = internalSource;
    }

    public io.xpipe.core.source.DataSource<?> getInternalSource() {
        return internalSource;
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
