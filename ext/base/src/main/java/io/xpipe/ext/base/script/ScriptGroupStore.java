package io.xpipe.ext.base.script;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.ext.base.GroupStore;
import io.xpipe.ext.base.SelfReferentialStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashSet;
import java.util.List;

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
    protected void queryFlattenedScripts(LinkedHashSet<SimpleScriptStore> all) {
        getEffectiveScripts().forEach(simpleScriptStore -> {
            simpleScriptStore.getStore().queryFlattenedScripts(all);
        });
    }

    @Override
    public List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts() {
        var self = getSelfEntry();
        return DataStorage.get().getDeepStoreChildren(self).stream()
                .map(dataStoreEntry -> dataStoreEntry.<ScriptStore>ref())
                .toList();
    }
}
