package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.source.TableDataSource;
import io.xpipe.core.source.TableReadConnection;
import io.xpipe.core.source.TableWriteConnection;
import io.xpipe.core.source.WriteMode;
import io.xpipe.core.store.StreamDataStore;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("xpbt")
@SuperBuilder
@Jacksonized
public class XpbtSource extends TableDataSource<StreamDataStore> {

    @Override
    protected TableWriteConnection newWriteConnection(WriteMode mode) {
        return new XpbtWriteConnection(this);
    }

    @Override
    protected TableReadConnection newReadConnection() {
        return new XpbtReadConnection(this);
    }
}
