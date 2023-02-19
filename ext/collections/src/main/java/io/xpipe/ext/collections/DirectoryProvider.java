package io.xpipe.ext.collections;

import io.xpipe.app.ext.DataSourceProviders;
import io.xpipe.app.util.UniformDataSourceProvider;
import io.xpipe.core.impl.LocalDirectoryDataStore;
import io.xpipe.core.source.*;
import io.xpipe.core.store.DataStore;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class DirectoryProvider implements UniformDataSourceProvider<DirectoryProvider.Source> {

    @Override
    public Source createDefaultSource(DataStore input) throws Exception {
        return Source.builder().store(input.asNeeded()).build();
    }

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.COLLECTION;
    }

    @Override
    public boolean prefersStore(DataStore store, DataSourceType type) {
        return store instanceof LocalDirectoryDataStore;
    }

    @Override
    public boolean couldSupportStore(DataStore store) {
        return store instanceof LocalDirectoryDataStore;
    }

    @Override
    public String getId() {
        return "dir";
    }

    @Override
    public Class<Source> getSourceClass() {
        return Source.class;
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("dir", "directory");
    }

    @SuperBuilder
    static class Source extends CollectionDataSource<LocalDirectoryDataStore> {

        @Override
        public Optional<String> determineDefaultName() {
            return Optional.of(getStore().getPath().getFileName().toString());
        }

        @Override
        public CollectionWriteConnection newWriteConnection(WriteMode mode) {
            return new CollectionWriteConnection() {
                @Override
                public void write(String entry, InputStream content) throws Exception {}

                @Override
                public void init() throws Exception {}

                @Override
                public void close() throws Exception {}
            };
        }

        @Override
        public CollectionReadConnection newReadConnection() {
            return new CollectionReadConnection() {
                @Override
                public boolean canRead() throws Exception {
                    return true;
                }

                @Override
                public Stream<DataSource<?>> listEntries() throws Exception {
                    var entries = new ArrayList<ArchiveEntryDataStore>();
                    Files.walkFileTree(store.getPath(), new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            var rel = store.getPath().relativize(file);
                            var name = rel.toString();
                            var dir = Files.isDirectory(file);
                            var e = new ArchiveEntryDataStore(dir, name, store) {
                                @Override
                                public InputStream openInput() throws Exception {
                                    return Files.newInputStream(file);
                                }

                                @Override
                                public OutputStream openOutput() throws Exception {
                                    return Files.newOutputStream(file);
                                }

                                @Override
                                public boolean canOpen() {
                                    return true;
                                }
                            };
                            entries.add(e);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    return entries.stream().map(archiveEntryDataStore -> DataSourceProviders.createDefault(archiveEntryDataStore));
                }
            };
        }
    }
}
