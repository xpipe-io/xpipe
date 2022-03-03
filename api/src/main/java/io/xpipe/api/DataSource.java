package io.xpipe.api;

import io.xpipe.api.impl.DataSourceImpl;
import io.xpipe.core.source.DataSourceConfig;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.source.DataSourceType;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

/**
 * Represents a reference to a data source that is managed by X-Pipe.
 *
 * The actual data is only queried when required and is not cached.
 * Therefore, the queried data is always up-to-date at the point of calling a method that queries the data.
 *
 * As soon a data source reference is created, the data source is locked
 * within X-Pipe to prevent concurrent modification and the problems that can arise from it.
 * By default, the lock is held until the calling program terminates and prevents
 * other applications from modifying the data source in any way.
 * To unlock the data source earlier, you can make use the {@link #unlock()} method.
 */
public interface DataSource {

    /**
     * NOT YET IMPLEMENTED!
     *
     * Creates a new supplier data source that will be interpreted as the generated data source.
     * In case this program should be a data source generator, this method has to be called at
     * least once to register that it actually generates a data source.
     *
     * All content that is written to this data source until the generator program terminates is
     * will be available later on when the data source is used as a supplier later on.
     *
     * In case this method is called multiple times, the same data source is returned.
     *
     * @return the generator data source
     */
    @Deprecated
    static DataSource supplySource() {
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
     * Retrieves a reference to the given data source.
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

    static DataSource wrap(InputStream in, String type, Map<String, String> configOptions) {
        return DataSourceImpl.wrap(in, type, configOptions);
    }

    static DataSource wrap(InputStream in, String type) {
        return DataSourceImpl.wrap(in, type, Map.of());
    }

    static DataSource wrap(InputStream in) {
        return DataSourceImpl.wrap(in, null, Map.of());
    }

    /**
     * Retrieves a reference to the given local data source that is specified by a URL.
     *
     * This wrapped data source is only available temporarily and locally,
     * i.e. it is not added to the XPipe data source storage.
     *
     * @param url the url that points to the data
     * @param type the data source type
     * @param configOptions additional configuration options for the specific data source type
     * @return a reference to the data source that can be used to access the underlying data source
     */
    static DataSource wrap(URL url, String type, Map<String, String> configOptions) {
        return DataSourceImpl.wrap(url, type, configOptions);
    }

    /**
     * Wrapper for {@link #wrap(URL, String, Map)} that passes no configuration options.
     * As a result, the data source configuration is automatically determined by X-Pipe for the given type.
     */
    static DataSource wrap(URL url, String type) {
        return wrap(url, type, Map.of());
    }

    /**
     * Wrapper for {@link #wrap(URL, String, Map)} that passes no type and no configuration options.
     * As a result, the data source type and configuration is automatically determined by X-Pipe.
     */
    static DataSource wrap(URL url) {
        return wrap(url, null, Map.of());
    }

    DataSourceId getId();

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
