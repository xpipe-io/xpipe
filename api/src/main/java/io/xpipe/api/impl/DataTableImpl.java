package io.xpipe.api.impl;

import io.xpipe.api.DataTable;
import io.xpipe.api.connector.XPipeApiConnector;
import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.ConnectorException;
import io.xpipe.beacon.ServerException;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.typed.TypedAbstractReader;
import io.xpipe.core.data.typed.TypedDataStreamParser;
import io.xpipe.core.data.typed.TypedReusableDataStructureNodeReader;
import io.xpipe.core.source.DataSourceConfig;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceInfo;
import io.xpipe.core.source.DataSourceType;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DataTableImpl extends DataSourceImpl implements DataTable {

    private final DataSourceInfo.Table info;

    DataTableImpl(DataSourceId id, DataSourceConfig sourceConfig, DataSourceInfo.Table info) {
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
        int maxToRead = info.getRowCount() == -1 ? maxRows : Math.min(info.getRowCount(), maxRows);

        List<DataStructureNode> nodes = new ArrayList<>();
        new XPipeApiConnector() {
            @Override
            protected void handle(BeaconClient sc) throws ClientException, ServerException, ConnectorException {
//                var req = ReadTableDataExchange.Request.builder()
//                        .sourceId(id).maxRows(maxToRead).build();
//                performInputExchange(sc, req, (ReadTableDataExchange.Response res, InputStream in) -> {
//                    var r = new TypedDataStreamParser(info.getDataType());
//                    r.parseStructures(in, TypedDataStructureNodeReader.immutable(info.getDataType()), nodes::add);
//                });
            }
        }.execute();
        return ArrayNode.of(nodes);
    }

    @Override
    public Iterator<TupleNode> iterator() {
        return new Iterator<>() {

            private InputStream input;
            private int read;
            private final int toRead = info.getRowCount();
            private final TypedDataStreamParser parser;
            private final TypedAbstractReader nodeReader;

            {
                new XPipeApiConnector() {
                    @Override
                    protected void handle(BeaconClient sc) throws ClientException, ServerException, ConnectorException {
//                        var req = ReadTableDataExchange.Request.builder()
//                                .sourceId(id).maxRows(Integer.MAX_VALUE).build();
//                        performInputExchange(sc, req,
//                                (ReadTableDataExchange.Response res, InputStream in) -> {
//                            input = in;
//                                });
                    }
                }.execute();

                nodeReader = TypedReusableDataStructureNodeReader.create(info.getDataType());
                parser = new TypedDataStreamParser(info.getDataType());
            }

            private void finish() {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            private boolean hasKnownSize() {
                return info.getRowCount() != -1;
            }

            @Override
            public boolean hasNext() {
                if (hasKnownSize() && read == toRead) {
                    finish();
                    return false;
                }

                if (hasKnownSize() && read < toRead) {
                    return true;
                }

                try {
                    var hasNext = parser.hasNext(input);
                    if (!hasNext) {
                        finish();
                    }
                    return hasNext;
                } catch (IOException ex) {
                    finish();
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
