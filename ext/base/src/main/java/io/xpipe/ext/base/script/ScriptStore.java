package io.xpipe.ext.base.script;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.DataStoreState;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.StatefulDataStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.core.util.XPipeInstallation;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@SuperBuilder
@Getter
@AllArgsConstructor
public abstract class ScriptStore extends JacksonizedValue implements DataStore, StatefulDataStore<ScriptStore.State> {

    public static ShellControl controlWithDefaultScripts(ShellControl pc) {
        return controlWithScripts(pc, getDefaultInitScripts(), getDefaultBringScripts());
    }

    public static ShellControl controlWithScripts(ShellControl pc, List<DataStoreEntryRef<ScriptStore>> initScripts, List<DataStoreEntryRef<ScriptStore>> bringScripts) {
        pc.onInit(shellControl -> {
            var initFlattened = flatten(initScripts);
            var scripts = initFlattened.stream()
                    .map(simpleScriptStore -> simpleScriptStore.prepareDumbScript(shellControl))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));
            if (!scripts.isBlank()) {
                shellControl.executeSimpleBooleanCommand(scripts);
            }

            var terminalCommands = initFlattened.stream()
                    .map(simpleScriptStore -> simpleScriptStore.prepareTerminalScript(shellControl))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));
            if (!terminalCommands.isBlank()) {
                shellControl.initWithTerminal(terminalCommands);
            }
        });
        pc.onInit(shellControl -> {
            var bringFlattened = flatten(bringScripts);
            var dir = initScriptsDirectory(shellControl, bringFlattened);
            if (dir != null) {
                shellControl.initWithTerminal(shellControl.getShellDialect().appendToPathVariableCommand(dir));
            }
        });
        return pc;
    }

    private static String initScriptsDirectory(ShellControl proc, List<SimpleScriptStore> scriptStores) throws Exception {
        if (scriptStores.size() == 0) {
            return null;
        }

        var refs = scriptStores.stream().map(scriptStore -> {
            return DataStorage.get().getStoreEntries().stream().filter(dataStoreEntry -> dataStoreEntry.getStore() == scriptStore).findFirst().orElseThrow().<SimpleScriptStore>ref();
        }).toList();
        var hash = refs.stream().mapToInt(value -> value.get().getName().hashCode() + value.getStore().hashCode()).sum();
        var xpipeHome = XPipeInstallation.getDataDir(proc);
        var targetDir = FileNames.join(xpipeHome, "scripts");
        var hashFile = FileNames.join(targetDir, "hash");
        var d = proc.getShellDialect();
        if (d.createFileExistsCommand(proc, hashFile).executeAndCheck()) {
            var read = d.getFileReadCommand(proc, hashFile).readStdoutOrThrow();
            var readHash = Integer.parseInt(read);
            if (hash == readHash) {
                return targetDir;
            }
        }

        d.deleteFileOrDirectory(proc, targetDir).execute();
        proc.executeSimpleCommand(d.getMkdirsCommand(targetDir));

        for (DataStoreEntryRef<SimpleScriptStore> scriptStore : refs) {
            var content = d.prepareScriptContent(scriptStore.getStore().getCommands());
            var fileName = scriptStore.get().getName().toLowerCase(Locale.ROOT).replaceAll(" ", "_");
            var scriptFile = FileNames.join(targetDir, fileName + "." + d.getScriptFileEnding());
            d.createScriptTextFileWriteCommand(proc, content, scriptFile).execute();

            var chmod = d.getScriptPermissionsCommand(scriptFile);
            proc.executeSimpleBooleanCommand(chmod);
        }

        d.createTextFileWriteCommand(proc, String.valueOf(hash), hashFile).execute();
        return targetDir;
    }

    public static List<DataStoreEntryRef<ScriptStore>> getDefaultInitScripts() {
        return DataStorage.get().getStoreEntries().stream()
                .filter(dataStoreEntry -> dataStoreEntry.getStore() instanceof ScriptStore scriptStore
                        && scriptStore.getState().isDefault())
                .map(e -> e.<ScriptStore>ref())
                .toList();
    }

    public static List<DataStoreEntryRef<ScriptStore>> getDefaultBringScripts() {
        return DataStorage.get().getStoreEntries().stream()
                .filter(dataStoreEntry -> dataStoreEntry.getStore() instanceof ScriptStore scriptStore
                        && scriptStore.getState().isBringToShell())
                .map(e -> e.<ScriptStore>ref())
                .toList();
    }

    public static List<SimpleScriptStore> flatten(List<DataStoreEntryRef<ScriptStore>> scripts) {
        var seen = new LinkedHashSet<SimpleScriptStore>();
        scripts.forEach(scriptStoreDataStoreEntryRef ->
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
        boolean bringToShell;
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

        // Prevent possible stack overflow
//        for (DataStoreEntryRef<ScriptStore> s : getEffectiveScripts()) {
//         s.checkComplete();
//        }
    }

    public LinkedHashSet<SimpleScriptStore> getFlattenedScripts() {
        var set = new LinkedHashSet<SimpleScriptStore>();
        queryFlattenedScripts(set);
        return set;
    }

    protected abstract void queryFlattenedScripts(LinkedHashSet<SimpleScriptStore> all);

    public abstract List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts();
}
