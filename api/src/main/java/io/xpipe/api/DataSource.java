package io.xpipe.api;

import io.xpipe.api.impl.DataSourceImpl;
import io.xpipe.core.source.DataStoreId;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents a reference to a data source that is managed by XPipe.
 * <p>
 * The actual data is only queried when required and is not cached.
 * Therefore, the queried data is always up-to-date at the point of calling a method that queries the data.
 * <p>
 * As soon a data source reference is created, the data source is locked
 * within XPipe to prevent concurrent modification and the problems that can arise from it.
 * By default, the lock is held until the calling program terminates and prevents
 * other applications from modifying the data source in any way.
 * To unlock the data source earlier, you can make use the {@link #unlock()} method.
 */
public interface DataSource {

    /**
     * NOT YET IMPLEMENTED!
     * <p>
     * Creates a new supplier data source that will be interpreted as the generated data source.
     * In case this program should be a data source generator, this method has to be called at
     * least once to register that it actually generates a data source.
     * <p>
     * All content that is written to this data source until the generator program terminates is
     * will be available later on when the data source is used as a supplier later on.
     * <p>
     * In case this method is called multiple times, the same data source is returned.
     *
     * @return the generator data source
     */
    static DataSource drain() {
        return null;
    }

    /**
     * NOT YET IMPLEMENTED!
     * <p>
     * Creates a data source sink that will block with any read operations
     * until an external data producer routes the output into this sink.
     */
    static DataSource sink() {
        return null;
    }

    /**
     * Wrapper for {@link #get(DataSourceReference)}.
     *
     * @throws IllegalArgumentException if {@code id} is not a valid data source id
     */
    static DataSource getById(String id) {
        return get(DataSourceReference.id(id));
    }

    /**
     * Wrapper for {@link #get(DataSourceReference)} using the latest reference.
     */
    static DataSource getLatest() {
        return get(DataSourceReference.latest());
    }

    /**
     * Wrapper for {@link #get(DataSourceReference)} using a name reference.
     */
    static DataSource getByName(String name) {
        return get(DataSourceReference.name(name));
    }

    /**
     * Retrieves the data source for a given reference.
     *
     * @param ref the data source reference
     */
    static DataSource get(DataSourceReference ref) {
        return DataSourceImpl.get(ref);
    }

    /**
     * Releases the lock held by this program for this data source such
     * that other applications can modify the data source again.
     */
    static void unlock() {
        throw new UnsupportedOperationException();
    }

    /**
     * Wrapper for {@link #create(DataStoreId, String, InputStream)} that creates an anonymous data source.
     */
    static DataSource createAnonymous(String type, Path path) {
        return create(null, type, path);
    }

    /**
     * Wrapper for {@link #create(DataStoreId, String, InputStream)}.
     */
    static DataSource create(DataStoreId id, String type, Path path) {
        try (var in = Files.newInputStream(path)) {
            return create(id, type, in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Wrapper for {@link #create(DataStoreId, String, InputStream)} that creates an anonymous data source.
     */
    static DataSource createAnonymous(String type, URL url) {
        return create(null, type, url);
    }

    /**
     * Wrapper for {@link #create(DataStoreId, String, InputStream)}.
     */
    static DataSource create(DataStoreId id, String type, URL url) {
        try (var in = url.openStream()) {
            return create(id, type, in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Wrapper for {@link #create(DataStoreId, String, InputStream)} that creates an anonymous data source.
     */
    static DataSource createAnonymous(String type, InputStream in) {
        return create(null, type, in);
    }

    /**
     * Creates a new data source from an input stream.
     *
     * @param id   the data source id
     * @param type the data source type
     * @param in   the input stream to read
     * @return a {@link DataSource} instances that can be used to access the underlying data
     */
    static DataSource create(DataStoreId id, String type, InputStream in) {
        return DataSourceImpl.create(id, type, in);
    }

    /**
     * Creates a new data source from an input stream.
     *
     * @param id the data source id
     * @return a {@link DataSource} instances that can be used to access the underlying data
     */
    static DataSource create(DataStoreId id, io.xpipe.core.source.DataSource<?> source) {
        return DataSourceImpl.create(id, source);
    }

    /**
     * Creates a new data source from an input stream.
     * 1
     *
     * @param id   the data source id
     * @param type the data source type
     * @param in   the data store to add
     * @return a {@link DataSource} instances that can be used to access the underlying data
     */
    static DataSource create(DataStoreId id, String type, DataStore in) {
        return DataSourceImpl.create(id, type, in);
    }

    void forwardTo(DataSource target);

    void appendTo(DataSource target);

    io.xpipe.core.source.DataSource<?> getInternalSource();

    /**
     * Returns the id of this data source.
     */
    DataStoreId getId();

    /**
     * Returns the type of this data source.
     */
    DataSourceType getType();

    DataSourceConfig getConfig();

    /**
     * Attempts to cast this object to a {@link DataTable}.
     *
     * @throws UnsupportedOperationException if the data source is not a table
     */
    default DataTable asTable() {
        throw new UnsupportedOperationException("Data source is not a table");
    }

    /**
     * Attempts to cast this object to a {@link DataStructure}.
     *
     * @throws UnsupportedOperationException if the data source is not a structure
     */
    default DataStructure asStructure() {
        throw new UnsupportedOperationException("Data source is not a structure");
    }

    /**
     * Attempts to cast this object to a {@link DataText}.
     *
     * @throws UnsupportedOperationException if the data source is not a text
     */
    default DataText asText() {
        throw new UnsupportedOperationException("Data source is not a text");
    }

    /**
     * Attempts to cast this object to a {@link DataRaw}.
     *
     * @throws UnsupportedOperationException if the data source is not raw
     */
    default DataRaw asRaw() {
        throw new UnsupportedOperationException("Data source is not raw");
    }
}
