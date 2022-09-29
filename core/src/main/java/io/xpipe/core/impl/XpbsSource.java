package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.source.StructureDataSource;
import io.xpipe.core.source.StructureReadConnection;
import io.xpipe.core.source.StructureWriteConnection;
import io.xpipe.core.store.StreamDataStore;
import lombok.experimental.SuperBuilder;

@JsonTypeName("xpbs")
@SuperBuilder
public class XpbsSource extends StructureDataSource<StreamDataStore> {

    @Override
    protected StructureWriteConnection newWriteConnection() {
        return null;
    }

    @Override
    protected StructureReadConnection newReadConnection() {
        return null;
    }
}
