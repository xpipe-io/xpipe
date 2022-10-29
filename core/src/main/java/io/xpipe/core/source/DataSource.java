package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.impl.TextSource;
import io.xpipe.core.impl.XpbtSource;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.JacksonizedValue;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;

import java.util.Optional;

/**
 * Represents a formal description on what exactly makes up the
 * actual data source and how to access/locate it for a given data store.
 * <p>
 * This instance is only valid in combination with its associated data store instance.
 */
@SuperBuilder
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class DataSource<DS extends DataStore> extends JacksonizedValue {

    protected DS store;

    public static DataSource<?> createInternalDataSource(DataSourceType t, DataStore store) {
        try {
            return switch (t) {
                case TABLE -> XpbtSource.builder().store(store.asNeeded()).build();
                case STRUCTURE -> null;
                case TEXT -> TextSource.builder()
                        .store(store.asNeeded())
                        .newLine(NewLine.LF)
                        .charset(StreamCharset.UTF8)
                        .build();
                case RAW -> null;
                    // TODO
                case COLLECTION -> null;
            };
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
    public void test() throws Exception {
        store.validate();
    }

    public void checkComplete() throws Exception {
        if (store == null) {
            throw new IllegalStateException("Store cannot be null");
        }

        store.checkComplete();
    }

    public WriteMode[] getAvailableWriteModes() {
        if (getFlow() != null && !getFlow().hasOutput()) {
            return new WriteMode[0];
        }

        return WriteMode.values();
    }

    public DataFlow getFlow() {
        if (store == null) {
            return null;
        }

        return store.getFlow();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T extends DataSource<DS>> T copy() {
        var mapper = JacksonMapper.newMapper();
        TokenBuffer tb = new TokenBuffer(mapper, false);
        mapper.writeValue(tb, this);
        return (T) mapper.readValue(tb.asParser(), getClass());
    }

    public DataSource<DS> withStore(DS store) {
        var c = copy();
        c.store = store;
        return c;
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

    public DataSourceReadConnection openReadConnection() throws Exception {
        throw new UnsupportedOperationException();
    }

    public DataSourceConnection openWriteConnection() throws Exception {
        throw new UnsupportedOperationException();
    }

    public DataSourceConnection openAppendingWriteConnection() throws Exception {
        throw new UnsupportedOperationException("Appending write is not supported");
    }

    public DataSourceConnection openPrependingWriteConnection() throws Exception {
        throw new UnsupportedOperationException("Prepending write is not supported");
    }

    public DS getStore() {
        return store;
    }
}
