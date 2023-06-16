package io.xpipe.api.impl;

import io.xpipe.api.DataSourceConfig;
import io.xpipe.api.DataTable;
import io.xpipe.api.connector.XPipeApiConnection;
import io.xpipe.beacon.BeaconConnection;
import io.xpipe.beacon.BeaconException;
import io.xpipe.beacon.exchange.api.QueryTableDataExchange;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.typed.TypedAbstractReader;
import io.xpipe.core.data.typed.TypedDataStreamParser;
import io.xpipe.core.data.typed.TypedDataStructureNodeReader;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.source.DataSourceType;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DataTableImpl extends DataSourceImpl implements DataTable {

    DataTableImpl(DataSourceId id, DataSourceConfig sourceConfig, io.xpipe.core.source.DataSource<?> internalSource) {
        super(id, sourceConfig, internalSource);
    }

    @Override
    public DataTable asTable() {
        return this;
    }

    public Stream<TupleNode> stream() {
        var iterator = new TableIterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .onClose(iterator::finish);
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
        XPipeApiConnection.execute(con -> {
            var req = QueryTableDataExchange.Request.builder()
                    .ref(DataSourceReference.id(getId()))
                    .maxRows(maxRows)
                    .build();
            con.performInputExchange(req, (QueryTableDataExchange.Response res, InputStream in) -> {
                var r = new TypedDataStreamParser(res.getDataType());

                r.parseStructures(in, TypedDataStructureNodeReader.of(res.getDataType()), nodes::add);
            });
        });
        return ArrayNode.of(nodes);
    }

    @Override
    public Iterator<TupleNode> iterator() {
        return new TableIterator();
    }

    private class TableIterator implements Iterator<TupleNode> {

        private final BeaconConnection connection;
        private final TypedDataStreamParser parser;
        private final TypedAbstractReader nodeReader;
        private TupleNode node;

        {
            connection = XPipeApiConnection.open();
            var req = QueryTableDataExchange.Request.builder()
                    .ref(DataSourceReference.id(getId()))
                    .maxRows(Integer.MAX_VALUE)
                    .build();
            connection.sendRequest(req);
            QueryTableDataExchange.Response response = connection.receiveResponse();

            nodeReader = TypedDataStructureNodeReader.of(response.getDataType());
            parser = new TypedDataStreamParser(response.getDataType());

            connection.receiveBody();
        }

        private void finish() {
            connection.close();
        }

        @Override
        public boolean hasNext() {
            connection.checkClosed();

            try {
                node = (TupleNode) parser.parseStructure(connection.getInputStream(), nodeReader);
            } catch (IOException e) {
                throw new BeaconException(e);
            }
            if (node == null) {
                // finish();
            }
            return node != null;
        }

        @Override
        public TupleNode next() {
            connection.checkClosed();

            return node;
        }
    }
}
