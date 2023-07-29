package io.xpipe.api.impl;

import io.xpipe.api.DataSourceConfig;
import io.xpipe.api.DataText;
import io.xpipe.core.source.DataStoreId;
import io.xpipe.core.source.DataSourceType;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataTextImpl extends DataSourceImpl implements DataText {

    DataTextImpl(
            DataStoreId sourceId, DataSourceConfig sourceConfig, io.xpipe.core.source.DataSource<?> internalSource) {
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
        return Stream.of();
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
