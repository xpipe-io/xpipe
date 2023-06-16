package io.xpipe.ext.collections;

import io.xpipe.app.ext.DataSourceProviders;
import io.xpipe.core.source.CollectionReadConnection;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.StreamDataStore;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class ArchiveReadConnection implements CollectionReadConnection {

    private final String type;
    private final StreamDataStore store;
    private ArchiveInputStream inputStream;

    public ArchiveReadConnection(String type, StreamDataStore store) {
        this.type = type;
        this.store = store;
    }

    @Override
    public Stream<DataSource<?>> listEntries() throws Exception {
        var ar = inputStream.getNextEntry();
        AtomicReference<ArchiveEntryStore> entry = new AtomicReference<>(
                ar != null ? new ArchiveEntryStore(store, ar.isDirectory(), this, ar.getName()) : null);
        if (entry.get() == null) {
            return Stream.empty();
        }

        return Stream.iterate(
                        entry.get(),
                        e -> {
                            ArchiveEntry next;
                            try {
                                next = inputStream.getNextEntry();
                            } catch (IOException ex) {
                                throw new UncheckedIOException(ex);
                            }

                            if (next == null) {
                                return false;
                            }

                            var dir = next.isDirectory();
                            entry.set(new ArchiveEntryStore(store, dir, this, next.getName()));
                            return true;
                        },
                        e -> {
                            return entry.get();
                        })
                .map(archiveEntryStore -> {
                    var preferred = DataSourceProviders.byPreferredStore(archiveEntryStore, null);
                    try {
                        return preferred.isPresent()
                                ? preferred
                                        .get()
                                        .createDefaultSource(archiveEntryStore)
                                        .asNeeded()
                                : null;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public void init() throws Exception {
        var in = store.openBufferedInput();
        inputStream = new ArchiveStreamFactory(type).createArchiveInputStream(in);
    }

    @Override
    public void close() throws Exception {
        inputStream.close();
    }

    public ArchiveInputStream getInputStream() {
        return inputStream;
    }

    @Override
    public boolean canRead() throws Exception {
        return store.canOpen();
    }
}
