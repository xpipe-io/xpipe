package io.xpipe.ext.base.script;

import io.xpipe.app.ext.GroupStore;
import io.xpipe.app.ext.SelfReferentialStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashSet;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
@Jacksonized
@JsonTypeName("scriptGroup")
public class ScriptGroupStore extends ScriptStore implements GroupStore<ScriptStore>, SelfReferentialStore {

    @Override
    public DataStoreEntryRef<? extends ScriptStore> getParent() {
        return group;
    }

    @Override
    protected void queryFlattenedScripts(LinkedHashSet<DataStoreEntryRef<SimpleScriptStore>> all) {
        getEffectiveScripts().forEach(simpleScriptStore -> {
            simpleScriptStore.getStore().queryFlattenedScripts(all);
        });
    }

    public List<DataStoreEntryRef<ScriptStore>> getImmediateChildrenScripts() {
        var self = getSelfEntry();
        return DataStorage.get().getStoreChildren(self).stream()
                .filter(entry -> entry.getValidity().isUsable())
                .map(dataStoreEntry -> dataStoreEntry.<ScriptStore>ref())
                .toList();
    }

    @Override
    public List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts() {
        var self = getSelfEntry();
        return DataStorage.get().getDeepStoreChildren(self).stream()
                .filter(entry -> entry.getValidity().isUsable())
                .map(dataStoreEntry -> dataStoreEntry.<ScriptStore>ref())
                .toList();
    }
}
