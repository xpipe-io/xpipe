package io.xpipe.core.source;

import com.fasterxml.jackson.databind.util.TokenBuffer;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.util.Optional;

/**
 * Represents a formal description on what exactly makes up the
 * actual data source and how to access/locate it for a given data store.
 *
 * This instance is only valid in combination with its associated data store instance.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class DataSource<DS extends DataStore> {

    protected DS store;

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T extends DataSource<DS>> T copy() {
        var mapper = JacksonHelper.newMapper();
        TokenBuffer tb = new TokenBuffer(mapper, false);
        mapper.writeValue(tb, this);
        return (T) mapper.readValue(tb.asParser(), getClass());
    }

    public DataSource<DS> withStore(DS store) {
        var c = copy();
        c.store = store;
        return c;
    }

    public boolean isComplete() {
        return true;
    }

    /**
     * Casts this instance to the required type without checking whether a cast is possible.
     */
    @SuppressWarnings("unchecked")
    public final <DSD extends DataSource<?>> DSD asNeeded() {
        return (DSD) this;
    }

    /**
     * Determines on optional default name for this data store that is
     * used when determining a suitable default name for a data source.
     */
    public Optional<String> determineDefaultName() {
        return Optional.empty();
    }

    /**
     * Determines the data source info.
     * This is usually called only once on data source
     * creation as this process might be expensive.
     */
    public abstract DataSourceInfo determineInfo() throws Exception;

    public abstract DataSourceReadConnection openReadConnection() throws Exception;

    public abstract DataSourceConnection openWriteConnection() throws Exception;

    public DataSourceConnection openAppendingWriteConnection() throws Exception {
        throw new UnsupportedOperationException("Appending write is not supported");
    }

    public DS getStore() {
        return store;
    }
}
