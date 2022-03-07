package io.xpipe.api.impl;

import io.xpipe.api.DataSource;
import io.xpipe.api.DataTable;
import io.xpipe.api.DataTableAccumulator;
import io.xpipe.api.connector.XPipeConnection;
import io.xpipe.api.util.TypeDescriptor;
import io.xpipe.beacon.exchange.PreStoreExchange;
import io.xpipe.beacon.exchange.ReadExecuteExchange;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.typed.TypedDataStreamWriter;
import io.xpipe.core.source.DataSourceConfigInstance;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DataTableAccumulatorImpl implements DataTableAccumulator {

    private final XPipeConnection connection;
    private final TupleType type;
    private int rows;
    private TupleType writtenDescriptor;

    public DataTableAccumulatorImpl(TupleType type) {
        this.type = type;
        connection = XPipeConnection.open();
        connection.sendRequest(PreStoreExchange.Request.builder().build());
        connection.sendBody();
    }

    @Override
    public synchronized DataTable finish(DataSourceId id) {
        connection.withOutputStream(OutputStream::close);
        PreStoreExchange.Response res = connection.receiveResponse();
        connection.close();

        var req = ReadExecuteExchange.Request.builder()
                .target(id).dataStore(res.getStore()).config(DataSourceConfigInstance.xpbt()).build();
        XPipeConnection.execute(con -> {
            con.performSimpleExchange(req);
        });
        return DataSource.get(DataSourceReference.id(id)).asTable();
    }

    private void writeDescriptor() {
        if (writtenDescriptor != null) {
            return;
        }
        writtenDescriptor = TupleType.tableType(type.getNames());

        connection.withOutputStream(out -> {
            out.write((TypeDescriptor.create(type.getNames())).getBytes(StandardCharsets.UTF_8));
        });
    }

    @Override
    public synchronized void add(DataStructureNode row) {
        TupleNode toUse = type.matches(row) ? row.asTuple() : type.convert(row).orElseThrow().asTuple();
        connection.withOutputStream(out -> {
            writeDescriptor();
            TypedDataStreamWriter.writeStructure(out, toUse, writtenDescriptor);
            rows++;
        });
    }

    @Override
    public synchronized DataStructureNodeAcceptor<DataStructureNode> acceptor() {
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
