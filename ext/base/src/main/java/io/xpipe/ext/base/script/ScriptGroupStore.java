package io.xpipe.ext.base.script;

import io.xpipe.app.ext.EnabledStoreState;
import io.xpipe.app.ext.GroupStore;
import io.xpipe.app.ext.SelfReferentialStore;
import io.xpipe.app.ext.StatefulDataStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.Validators;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashSet;
import java.util.List;

@Value
@SuperBuilder
@Jacksonized
@JsonTypeName("scriptGroup")
public class ScriptGroupStore implements GroupStore<ScriptGroupStore>, SelfReferentialStore, StatefulDataStore<EnabledStoreState> {

    DataStoreEntryRef<ScriptGroupStore> group;

    String description;

    @Override
    public Class<EnabledStoreState> getStateClass() {
        return EnabledStoreState.class;
    }

    @Override
    public void checkComplete() throws Throwable {
        if (group != null) {
            Validators.isType(group, ScriptGroupStore.class);
        }
    }
    @Override
    public DataStoreEntryRef<ScriptGroupStore> getParent() {
        return group;
    }
}
