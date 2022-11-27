package io.xpipe.api.impl;

import io.xpipe.api.DataSourceConfig;
import io.xpipe.api.DataText;
import io.xpipe.api.connector.XPipeApiConnection;
import io.xpipe.beacon.BeaconConnection;
import io.xpipe.beacon.BeaconException;
import io.xpipe.beacon.exchange.api.QueryTextDataExchange;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.source.DataSourceType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DataTextImpl extends DataSourceImpl implements DataText {

     DataTextImpl(
            DataSourceId sourceId,
            DataSourceConfig sourceConfig,
            io.xpipe.core.source.DataSource<?> internalSource) {
        super(sourceId, sourceConfig, internalSource);
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.TEXT;
    }

    @Override
    public DataText asText() {
        return this;
    }

    @Override
    public List<String> readAllLines() {
        return readLines(Integer.MAX_VALUE);
    }

    @Override
    public List<String> readLines(int maxLines) {
        try (Stream<String> lines = lines()) {
            return lines.limit(maxLines).toList();
        }
    }

    @Override
    public Stream<String> lines() {
        var iterator = new Iterator<String>() {

            private final BeaconConnection connection;
            private final BufferedReader reader;
            private String nextValue;

            {
                connection = XPipeApiConnection.open();
                var req = QueryTextDataExchange.Request.builder()
                        .ref(DataSourceReference.id(getId()))
                        .maxLines(-1)
                        .build();
                connection.sendRequest(req);
                connection.receiveResponse();
                reader = new BufferedReader(new InputStreamReader(connection.receiveBody(), StandardCharsets.UTF_8));
            }

            private void close() {
                connection.close();
            }

            @Override
            public boolean hasNext() {
                connection.checkClosed();

                try {
                    nextValue = reader.readLine();
                } catch (IOException e) {
                    throw new BeaconException(e);
                }
                return nextValue != null;
            }

            @Override
            public String next() {
                return nextValue;
            }
        };

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .onClose(iterator::close);
    }

    @Override
    public String readAll() {
        try (Stream<String> lines = lines()) {
            return lines.collect(Collectors.joining("\n"));
        }
    }

    @Override
    public String read(int maxCharacters) {
        StringBuilder builder = new StringBuilder();
        lines().takeWhile(s -> {
            if (builder.length() > maxCharacters) {
                return false;
            }

            builder.append(s);
            return true;
        });
        return builder.toString();
    }
}
