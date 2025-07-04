package io.xpipe.ext.base.script;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.EnabledStoreState;
import io.xpipe.app.ext.StatefulDataStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.*;

@SuperBuilder(toBuilder = true)
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public abstract class ScriptStore implements DataStore, StatefulDataStore<EnabledStoreState> {

    protected final DataStoreEntryRef<ScriptGroupStore> group;

    @Singular
    protected final List<DataStoreEntryRef<ScriptStore>> scripts;

    protected final String description;

    @Override
    public Class<EnabledStoreState> getStateClass() {
        return EnabledStoreState.class;
    }

    @Override
    public void checkComplete() throws Throwable {
        if (group != null) {
            Validators.isType(group, ScriptGroupStore.class);
        }
        if (scripts != null) {
            Validators.contentNonNull(scripts);
        }

        // Prevent possible stack overflow
        //        for (DataStoreEntryRef<ScriptStore> s : getEffectiveScripts()) {
        //         s.checkComplete();
        //        }
    }

    SequencedCollection<DataStoreEntryRef<SimpleScriptStore>> queryFlattenedScripts() {
        var seen = new LinkedHashSet<DataStoreEntryRef<SimpleScriptStore>>();
        queryFlattenedScripts(seen);
        return seen;
    }

    protected abstract void queryFlattenedScripts(LinkedHashSet<DataStoreEntryRef<SimpleScriptStore>> all);

    public abstract List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts();
}
