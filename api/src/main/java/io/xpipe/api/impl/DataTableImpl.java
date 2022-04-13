package io.xpipe.api.impl;

import io.xpipe.api.DataTable;
import io.xpipe.api.connector.XPipeConnection;
import io.xpipe.beacon.BeaconConnection;
import io.xpipe.beacon.exchange.api.QueryTableDataExchange;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.typed.TypedAbstractReader;
import io.xpipe.core.data.typed.TypedDataStreamParser;
import io.xpipe.core.data.typed.TypedDataStructureNodeReader;
import io.xpipe.core.data.typed.TypedReusableDataStructureNodeReader;
import io.xpipe.core.source.*;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DataTableImpl extends DataSourceImpl implements DataTable {

    private final DataSourceInfo.Table info;

    DataTableImpl(DataSourceId id, DataSourceConfigInstance sourceConfig, DataSourceInfo.Table info) {
        super(id, sourceConfig);
        this.info = info;
    }

    @Override
    public DataTable asTable() {
        return this;
    }

    @Override
    public DataSourceInfo.Table getInfo() {
        return info;
    }

    public Stream<TupleNode> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.TABLE;
    }

    @Override
    public ArrayNode readAll() {
        return read(Integer.MAX_VALUE);
    }

    @Override
    public ArrayNode read(int maxRows) {
        List<DataStructureNode> nodes = new ArrayList<>();
        XPipeConnection.execute(con -> {
            var req = QueryTableDataExchange.Request.builder()
                    .ref(DataSourceReference.id(getId())).maxRows(maxRows).build();
            con.performInputExchange(req, (QueryTableDataExchange.Response res, InputStream in) -> {
                    var r = new TypedDataStreamParser(info.getDataType());
                    r.parseStructures(in, TypedDataStructureNodeReader.immutable(info.getDataType()), nodes::add);
            });
        });
        return ArrayNode.of(nodes);
    }

    @Override
    public Iterator<TupleNode> iterator() {
        return new Iterator<>() {

            private final BeaconConnection connection;
            private final TypedDataStreamParser parser;
            private final TypedAbstractReader nodeReader;

            {
                nodeReader = TypedReusableDataStructureNodeReader.create(info.getDataType());
                parser = new TypedDataStreamParser(info.getDataType());

                connection = XPipeConnection.open();
                var req = QueryTableDataExchange.Request.builder()
                        .ref(DataSourceReference.id(getId())).build();
                connection.sendRequest(req);
                connection.receiveResponse();
                connection.receiveBody();
            }

            private void finish() {
                connection.close();
            }

            @Override
            public boolean hasNext() {
                connection.checkClosed();

                AtomicBoolean hasNext = new AtomicBoolean(false);
                connection.withInputStream(in -> {
                    hasNext.set(parser.hasNext(in));
                });
                if (!hasNext.get()) {
                    finish();
                }
                return hasNext.get();
            }

            @Override
            public TupleNode next() {
                connection.checkClosed();

                AtomicReference<TupleNode> current = new AtomicReference<>();
                connection.withInputStream(in -> {
                    current.set((TupleNode) parser.parseStructure(connection.getInputStream(), nodeReader));
                });
                return current.get();
            }
        };
    }
}
