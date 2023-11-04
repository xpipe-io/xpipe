package io.xpipe.ext.base.script;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.ScriptSnippet;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.SimpleScriptSnippet;
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

@SuperBuilder
@Getter
@AllArgsConstructor
public abstract class ScriptStore extends JacksonizedValue implements DataStore, StatefulDataStore<ScriptStore.State> {

    protected final DataStoreEntryRef<ScriptGroupStore> group;
    @Singular
    protected final List<DataStoreEntryRef<ScriptStore>> scripts;
    protected final String description;

    public static ShellControl controlWithDefaultScripts(ShellControl pc) {
        return controlWithScripts(pc, getDefaultInitScripts(), getDefaultBringScripts());
    }

    public static ShellControl controlWithScripts(
            ShellControl pc, List<DataStoreEntryRef<ScriptStore>> initScripts, List<DataStoreEntryRef<ScriptStore>> bringScripts
    ) {
        var initFlattened = flatten(initScripts);
        var bringFlattened = flatten(bringScripts);

        pc.onInit(shellControl -> {
            passInitScripts(pc, initFlattened);

            var dir = initScriptsDirectory(shellControl, bringFlattened);
            if (dir != null) {
                shellControl.initWith(new SimpleScriptSnippet(shellControl.getShellDialect().appendToPathVariableCommand(dir),
                        ScriptSnippet.ExecutionType.TERMINAL_ONLY));
            }
        });
        return pc;
    }

    private static void passInitScripts(ShellControl pc, List<SimpleScriptStore> scriptStores) throws Exception {
        scriptStores.forEach(simpleScriptStore -> {
            if (pc.getInitCommands().contains(simpleScriptStore)) {
                return;
            }

            if (!simpleScriptStore.getMinimumDialect().isCompatibleTo(pc.getShellDialect())) {
                return;
            }

            pc.initWith(simpleScriptStore);
        });
    }

    private static String initScriptsDirectory(ShellControl proc, List<SimpleScriptStore> scriptStores) throws Exception {
        if (scriptStores.isEmpty()) {
            return null;
        }

        var applicable = scriptStores.stream().filter(
                simpleScriptStore -> simpleScriptStore.getMinimumDialect().isCompatibleTo(proc.getShellDialect())).toList();
        if (applicable.isEmpty()) {
            return null;
        }

        var refs = applicable.stream().map(scriptStore -> {
            return DataStorage.get().getStoreEntries().stream().filter(dataStoreEntry -> dataStoreEntry.getStore() == scriptStore).findFirst()
                    .orElseThrow().<SimpleScriptStore>ref();
        }).toList();
        var hash = refs.stream().mapToInt(value -> value.get().getName().hashCode() + value.getStore().hashCode()).sum();
        var xpipeHome = XPipeInstallation.getDataDir(proc);
        var targetDir = FileNames.join(xpipeHome, "scripts", proc.getShellDialect().getId());
        var hashFile = FileNames.join(targetDir, "hash");
        var d = proc.getShellDialect();
        if (d.createFileExistsCommand(proc, hashFile).executeAndCheck()) {
            var read = d.getFileReadCommand(proc, hashFile).readStdoutOrThrow();
            try {
                var readHash = Integer.parseInt(read);
                if (hash == readHash) {
                    return targetDir;
                }
            } catch (NumberFormatException e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        }

        if (d.directoryExists(proc, targetDir).executeAndCheck()) {
            d.deleteFileOrDirectory(proc, targetDir).execute();
        }
        proc.executeSimpleCommand(d.getMkdirsCommand(targetDir));

        for (DataStoreEntryRef<SimpleScriptStore> scriptStore : refs) {
            var content = d.prepareScriptContent(scriptStore.getStore().getCommands());
            var fileName = scriptStore.get().getName().toLowerCase(Locale.ROOT).replaceAll(" ", "_");
            var scriptFile = FileNames.join(targetDir, fileName + "." + d.getScriptFileEnding());
            d.createScriptTextFileWriteCommand(proc, content, scriptFile).execute();

            var chmod = d.getScriptPermissionsCommand(scriptFile);
            if (chmod != null) {
                proc.executeSimpleBooleanCommand(chmod);
            }
        }

        d.createTextFileWriteCommand(proc, String.valueOf(hash), hashFile).execute();
        return targetDir;
    }

    public static List<DataStoreEntryRef<ScriptStore>> getDefaultInitScripts() {
        return DataStorage.get().getStoreEntries().stream().filter(
                dataStoreEntry -> dataStoreEntry.getStore() instanceof ScriptStore scriptStore && scriptStore.getState().isDefault()).map(
                DataStoreEntry::<ScriptStore>ref).toList();
    }

    public static List<DataStoreEntryRef<ScriptStore>> getDefaultBringScripts() {
        return DataStorage.get().getStoreEntries().stream().filter(
                dataStoreEntry -> dataStoreEntry.getStore() instanceof ScriptStore scriptStore && scriptStore.getState().isBringToShell()).map(
                DataStoreEntry::<ScriptStore>ref).toList();
    }

    public static List<SimpleScriptStore> flatten(List<DataStoreEntryRef<ScriptStore>> scripts) {
        var seen = new LinkedHashSet<SimpleScriptStore>();
        scripts.forEach(scriptStoreDataStoreEntryRef -> scriptStoreDataStoreEntryRef.getStore().queryFlattenedScripts(seen));
        return seen.stream().toList();
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

    protected abstract void queryFlattenedScripts(LinkedHashSet<SimpleScriptStore> all);

    public abstract List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts();

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Setter
    @Getter
    @SuperBuilder
    @Jacksonized
    public static class State extends DataStoreState {
        boolean isDefault;
        boolean bringToShell;
    }
}
