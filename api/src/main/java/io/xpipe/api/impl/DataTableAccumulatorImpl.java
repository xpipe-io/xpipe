package io.xpipe.api.impl;

import io.xpipe.api.DataSource;
import io.xpipe.api.DataTable;
import io.xpipe.api.DataTableAccumulator;
import io.xpipe.api.connector.XPipeApiConnection;
import io.xpipe.api.util.TypeDescriptor;
import io.xpipe.beacon.BeaconException;
import io.xpipe.beacon.exchange.ReadExchange;
import io.xpipe.beacon.exchange.WriteStreamExchange;
import io.xpipe.beacon.exchange.cli.StoreAddExchange;
import io.xpipe.beacon.util.QuietDialogHandler;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.typed.TypedDataStreamWriter;
import io.xpipe.core.impl.InternalStreamStore;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DataTableAccumulatorImpl implements DataTableAccumulator {

    private final XPipeApiConnection connection;
    private final TupleType type;
    private int rows;
    private InternalStreamStore store;
    private TupleType writtenDescriptor;
    private OutputStream bodyOutput;

    public DataTableAccumulatorImpl(TupleType type) {
        this.type = type;
        connection = XPipeApiConnection.open();

        store = new InternalStreamStore();
        var addReq = StoreAddExchange.Request.builder().storeInput(store).name(store.getUuid().toString()).build();
        StoreAddExchange.Response addRes = connection.performSimpleExchange(addReq);
        QuietDialogHandler.handle(addRes.getConfig(), connection);

        connection.sendRequest(WriteStreamExchange.Request.builder().name(store.getUuid().toString()).build());
        bodyOutput = connection.sendBody();
    }

    @Override
    public synchronized DataTable finish(DataSourceId id) {
        try {
            bodyOutput.close();
        } catch (IOException e) {
            throw new BeaconException(e);
        }

        WriteStreamExchange.Response res = connection.receiveResponse();
        connection.close();

        var req = ReadExchange.Request.builder()
                .target(id)
                .store(store)
                .provider("xpbt")
                .configureAll(false)
                .build();
        ReadExchange.Response response = XPipeApiConnection.execute(con -> {
            return con.performSimpleExchange(req);
        });

        var configInstance = response.getConfig();
        XPipeApiConnection.finishDialog(configInstance);

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
        TupleNode toUse = type.matches(row)
                ? row.asTuple()
                : type.convert(row).orElseThrow().asTuple();
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
