package io.xpipe.ext.base.script;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.ext.base.GroupStore;
import io.xpipe.ext.base.SelfReferentialStore;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Set;

@Getter
@SuperBuilder
@Jacksonized
@JsonTypeName("scriptGroup")
public class ScriptGroupStore extends ScriptStore implements GroupStore<ScriptStore>, SelfReferentialStore {

    @Override
    public DataStoreEntryRef<? extends ScriptStore> getParent() {
        return group;
    }

    @Override
    public List<SimpleScriptStore> getFlattenedScripts(Set<SimpleScriptStore> seen) {
        return getEffectiveScripts().stream().map(scriptStoreDataStoreEntryRef -> {
            return scriptStoreDataStoreEntryRef.getStore().getFlattenedScripts(seen);
        }).flatMap(List::stream).toList();
    }

    @Override
    public List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts() {
        var self = getSelfEntry();
        return DataStorage.get().getStoreChildren(self, true).stream()
                .map(dataStoreEntry -> dataStoreEntry.<ScriptStore>ref())
                .toList();
    }
}
