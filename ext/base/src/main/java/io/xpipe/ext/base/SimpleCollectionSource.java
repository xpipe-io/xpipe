package io.xpipe.ext.base;

import io.xpipe.core.source.CollectionReadConnection;
import io.xpipe.core.source.DataSource;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.stream.Stream;

@SuperBuilder
public abstract class SimpleCollectionSource extends ReadOnlyCollectionSource {
    protected abstract List<DataSource<?>> get();

    @Override
    protected CollectionReadConnection newReadConnection() {
        return new CollectionReadConnection() {
            @Override
            public Stream<DataSource<?>> listEntries() {
                return get().stream();
            }

            @Override
            public boolean canRead() {
                return true;
            }
        };
    }
}
