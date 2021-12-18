package io.xpipe.api.impl;

import io.xpipe.api.DataTable;
import io.xpipe.api.XPipeApiConnector;
import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.ConnectorException;
import io.xpipe.beacon.ServerException;
import io.xpipe.beacon.exchange.ReadTableDataExchange;
import io.xpipe.beacon.exchange.ReadTableInfoExchange;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.typed.TypedAbstractReader;
import io.xpipe.core.data.typed.TypedDataStreamParser;
import io.xpipe.core.data.typed.TypedDataStructureNodeReader;
import io.xpipe.core.data.typed.TypedReusableDataStructureNodeReader;
import io.xpipe.core.source.DataSourceId;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DataTableImpl implements DataTable {

    public static DataTable get(String s) {
        return get(DataSourceId.fromString(s));
    }

    public static DataTable get(DataSourceId ds) {
        final DataTable[] table = {null};
        new XPipeApiConnector() {
            @Override
            protected void handle(BeaconClient sc) throws ClientException, ServerException, ConnectorException {
                var req = new ReadTableInfoExchange.Request(ds);
                ReadTableInfoExchange.Response res = performSimpleExchange(sc, req);
                table[0] = new DataTableImpl(res.sourceId(), res.rowCount(), res.dataType());
            }
        }.execute();
        return table[0];
    }

    private final DataSourceId id;
    private final int size;
    private final DataType dataType;

    public DataTableImpl(DataSourceId id, int size, DataType dataType) {
        this.id = id;
        this.size = size;
        this.dataType = dataType;
    }

    public Stream<TupleNode> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }

    @Override
    public DataSourceId getId() {
        return id;
    }

    @Override
    public int getRowCount() {
        if (size == -1) {
            throw new UnsupportedOperationException("Row count is unknown");
        }

        return size;
    }

    @Override
    public OptionalInt getRowCountIfPresent() {
        return size != -1 ? OptionalInt.of(size) : OptionalInt.empty();
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public ArrayNode readAll() {
        return read(Integer.MAX_VALUE);
    }

    @Override
    public ArrayNode read(int maxRows) {
        int maxToRead = size == -1 ? maxRows : Math.min(size, maxRows);

        List<DataStructureNode> nodes = new ArrayList<>();
        new XPipeApiConnector() {
            @Override
            protected void handle(BeaconClient sc) throws ClientException, ServerException, ConnectorException {
                var req = new ReadTableDataExchange.Request(id, maxToRead);
                performExchange(sc, req, (ReadTableDataExchange.Response res, InputStream in) -> {
                    var r = new TypedDataStreamParser(dataType);
                    r.parseStructures(in, TypedDataStructureNodeReader.immutable(dataType), nodes::add);
                }, false);
            }
        }.execute();
        return ArrayNode.of(nodes);
    }

    @Override
    public Iterator<TupleNode> iterator() {
        return new Iterator<>() {

            private InputStream input;
            private int read;
            private final int toRead = size;
            private final TypedDataStreamParser parser;
            private final TypedAbstractReader nodeReader;

            {
                new XPipeApiConnector() {
                    @Override
                    protected void handle(BeaconClient sc) throws ClientException, ServerException, ConnectorException {
                        var req = new ReadTableDataExchange.Request(id, Integer.MAX_VALUE);
                        performExchange(sc, req,
                                (ReadTableDataExchange.Response res, InputStream in) -> input = in, false);
                    }
                }.execute();

                nodeReader = TypedReusableDataStructureNodeReader.create(dataType);
                parser = new TypedDataStreamParser(dataType);
            }

            private boolean hasKnownSize() {
                return size != -1;
            }

            @Override
            public boolean hasNext() {
                if (hasKnownSize() && read == toRead) {
                    return false;
                }

                if (hasKnownSize() && read < toRead) {
                    return true;
                }

                try {
                    return parser.hasNext(input);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }

            @Override
            public TupleNode next() {
                TupleNode current;
                try {
                    current = (TupleNode) parser.parseStructure(input, nodeReader);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
                read++;
                return current;
            }
        };
    }
}
