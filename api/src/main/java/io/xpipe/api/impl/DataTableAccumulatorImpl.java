package io.xpipe.api.impl;

import io.xpipe.api.DataSource;
import io.xpipe.api.DataTable;
import io.xpipe.api.DataTableAccumulator;
import io.xpipe.api.connector.XPipeConnection;
import io.xpipe.beacon.exchange.PreStoreExchange;
import io.xpipe.beacon.exchange.ReadExecuteExchange;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.typed.TypedDataStreamWriter;
import io.xpipe.core.source.DataSourceConfigInstance;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;

public class DataTableAccumulatorImpl implements DataTableAccumulator {

    private final XPipeConnection connection;
    private final TupleType type;
    private int rows;

    public DataTableAccumulatorImpl(TupleType type) {
        this.type = type;
        connection = XPipeConnection.open();
        connection.sendRequest(PreStoreExchange.Request.builder().build());
        connection.sendBodyStart();
    }

    @Override
    public synchronized DataTable finish(DataSourceId id) {
        PreStoreExchange.Response res = connection.receiveResponse();
        connection.close();

        var req = ReadExecuteExchange.Request.builder()
                .target(id).dataStore(res.getStore()).config(DataSourceConfigInstance.xpbt()).build();
        XPipeConnection.execute(con -> {
            con.performSimpleExchange(req);
        });
        return DataSource.get(DataSourceReference.id(id)).asTable();
    }

    @Override
    public synchronized void add(TupleNode row) {
        connection.withOutputStream(out -> {
            TypedDataStreamWriter.writeStructure(connection.getOutputStream(), row, type);
            rows++;
        });
    }

    @Override
    public synchronized DataStructureNodeAcceptor<TupleNode> acceptor() {
        return node -> {
            add(node);
            return true;
        };
    }

    @Override
    public synchronized int getCurrentRows() {
        return rows;
    }
}
