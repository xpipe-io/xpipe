package io.xpipe.ext.base.script;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.ext.base.GroupStore;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@SuperBuilder
@Jacksonized
@JsonTypeName("scriptGroup")
public class ScriptGroupStore extends JacksonizedValue implements GroupStore<DataStore> {

    private final String description;

    @Override
    public DataStoreEntryRef<DataStore> getParent() {
        return null;
    }
}
