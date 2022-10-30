package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.source.StructureDataSource;
import io.xpipe.core.source.StructureReadConnection;
import io.xpipe.core.source.StructureWriteConnection;
import io.xpipe.core.source.WriteMode;
import io.xpipe.core.store.StreamDataStore;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("xpbs")
@SuperBuilder
@Jacksonized
public class XpbsSource extends StructureDataSource<StreamDataStore> {

    @Override
    protected StructureWriteConnection newWriteConnection(WriteMode mode) {
        return new XpbsWriteConnection(this);
    }

    @Override
    protected StructureReadConnection newReadConnection() {
        return new XpbsReadConnection(this);
    }
}
