package io.xpipe.api;

import io.xpipe.api.impl.DataSourceImpl;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceType;

import java.net.URL;
import java.util.Map;

/**
 * Represents a reference to an XPipe data source.
 *
 * The actual data is only queried when required and is not cached.
 * Therefore, the queried data is always up-to-date at the point of calling a method that queries the data.
 */
public interface DataSource {

    /**
     * Wrapper for {@link #get(DataSourceId)}.
     *
     * @throws IllegalArgumentException if {@code id} is not a valid data source id
     */
    static DataSource get(String id) {
        return get(DataSourceId.fromString(id));
    }

    /**
     * Retrieves a reference to the given data source.
     *
     * @param id the data source id
     * @return a reference to the data source that can be used to access the underlying data source
     */
    static DataSource get(DataSourceId id) {
        return DataSourceImpl.get(id);
    }

    /**
     * Retrieves a reference to the given local data source that is specified by a URL.
     *
     * This wrapped data source is only available temporarily and locally,
     * i.e. it is not added to the XPipe data source storage.
     *
     * @param url the url that points to the data
     * @param configOptions additional configuration options for the specific data source type
     * @return a reference to the data source that can be used to access the underlying data source
     */
    static DataSource wrap(URL url, Map<String, String> configOptions) {
        return null;
    }

    /**
     * Wrapper for {@link #wrap(URL, Map)} that passes no configuration options.
     */
    static DataSource wrap(URL url) {
        return wrap(url, Map.of());
    }

    DataSourceId getId();

    DataSourceType getType();

    /**
     * Attemps to cast this object to a {@link DataTable}.
     *
     * @throws UnsupportedOperationException if the data source is not a table
     */
    default DataTable asTable() {
        throw new UnsupportedOperationException("Data source is not a table");
    }
}
