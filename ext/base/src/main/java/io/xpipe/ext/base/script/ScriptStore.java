package io.xpipe.ext.base.script;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.DataStoreState;
import io.xpipe.core.store.StatefulDataStore;
import io.xpipe.core.util.JacksonizedValue;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.*;
import java.util.stream.Collectors;

@SuperBuilder
@Getter
@AllArgsConstructor
public abstract class ScriptStore extends JacksonizedValue implements DataStore, StatefulDataStore<ScriptStore.State> {

    public static ShellControl controlWithDefaultScripts(ShellControl pc) {
        return controlWithScripts(pc,getDefaultScripts());
    }

    public static ShellControl controlWithScripts(ShellControl pc, List<DataStoreEntryRef<ScriptStore>> refs) {
        pc.onInit(shellControl -> {
            var scripts = flatten(refs).stream()
                    .map(simpleScriptStore -> simpleScriptStore.prepareDumbScript(shellControl))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));
            if (!scripts.isBlank()) {
                shellControl.executeSimpleBooleanCommand(scripts);
            }

            var terminalCommands = flatten(refs).stream()
                    .map(simpleScriptStore -> simpleScriptStore.prepareTerminalScript(shellControl))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));
            if (!terminalCommands.isBlank()) {
                shellControl.initWithTerminal(terminalCommands);
            }
        });
        return pc;
    }

    private static List<DataStoreEntryRef<ScriptStore>> getDefaultScripts() {
        return DataStorage.get().getStoreEntries().stream()
                .filter(dataStoreEntry -> dataStoreEntry.getStore() instanceof ScriptStore scriptStore
                        && scriptStore.getState().isDefault())
                .map(e -> e.<ScriptStore>ref())
                .toList();
    }

    public static List<SimpleScriptStore> flatten(List<DataStoreEntryRef<ScriptStore>> scripts) {
        var seen = new LinkedHashSet<SimpleScriptStore>();
        scripts
                .forEach(scriptStoreDataStoreEntryRef ->
                        scriptStoreDataStoreEntryRef.getStore().queryFlattenedScripts(seen));
        return seen.stream().toList();
    }

    protected final DataStoreEntryRef<ScriptGroupStore> group;

    @Singular
    protected final List<DataStoreEntryRef<ScriptStore>> scripts;

    protected final String description;

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Setter
    @Getter
    @SuperBuilder
    @Jacksonized
    public static class State extends DataStoreState {
        boolean isDefault;
    }

    @Override
    public Class<State> getStateClass() {
        return State.class;
    }

    @Override
    public void checkComplete() throws Exception {
        Validators.isType(group, ScriptGroupStore.class);
        if (scripts != null) {
            Validators.contentNonNull(scripts);
        }
    }

    public LinkedHashSet<SimpleScriptStore> getFlattenedScripts() {
        var set = new LinkedHashSet<SimpleScriptStore>();
        queryFlattenedScripts(set);
        return set;
    }

    protected abstract void queryFlattenedScripts(LinkedHashSet<SimpleScriptStore> all);

    public abstract List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts();
}
