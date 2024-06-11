package io.xpipe.ext.base.script;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellInitCommand;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.EnabledStoreState;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.StatefulDataStore;
import io.xpipe.core.util.JacksonizedValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

import java.util.*;

@SuperBuilder
@Getter
@AllArgsConstructor
public abstract class ScriptStore extends JacksonizedValue implements DataStore, StatefulDataStore<EnabledStoreState> {

    protected final DataStoreEntryRef<ScriptGroupStore> group;

    @Singular
    protected final List<DataStoreEntryRef<ScriptStore>> scripts;

    protected final String description;

    public static ShellControl controlWithDefaultScripts(ShellControl pc) {
        return controlWithScripts(pc, getDefaultEnabledScripts());
    }

    public static ShellControl controlWithScripts(
            ShellControl pc,
            List<DataStoreEntryRef<ScriptStore>> enabledScripts) {
        try {
            // Don't copy scripts if we don't want to modify the file system
            if (!pc.getEffectiveSecurityPolicy().permitTempScriptCreation()) {
                return pc;
            }

            var initFlattened = flatten(enabledScripts).stream().filter(store -> store.isInitScript()).toList();
            var bringFlattened = flatten(enabledScripts).stream().filter(store -> store.isShellScript()).toList();

            // Optimize if we have nothing to do
            if (initFlattened.isEmpty() && bringFlattened.isEmpty()) {
                return pc;
            }

            initFlattened.forEach(simpleScriptStore -> {
                pc.withInitSnippet(simpleScriptStore);
            });
            if (!bringFlattened.isEmpty()) {
                pc.withInitSnippet(new ShellInitCommand() {

                    String dir;

                    @Override
                    public Optional<String> terminalContent(ShellControl shellControl) throws Exception {
                        if (dir == null) {
                            dir = initScriptsDirectory(shellControl, bringFlattened);
                        }

                        if (dir == null) {
                            return Optional.empty();
                        }

                        return Optional.ofNullable(shellControl.getShellDialect().addToPathVariableCommand(List.of(dir), true));
                    }

                    @Override
                    public boolean runInTerminal() {
                        return true;
                    }
                });
            }
            return pc;
        } catch (StackOverflowError t) {
            throw new RuntimeException("Unable to set up scripts. Is there a circular script dependency?", t);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to set up scripts", t);
        }
    }

    private static String initScriptsDirectory(ShellControl proc, List<SimpleScriptStore> scriptStores)
            throws Exception {
        if (scriptStores.isEmpty()) {
            return null;
        }

        var applicable = scriptStores.stream()
                .filter(simpleScriptStore ->
                        simpleScriptStore.getMinimumDialect().isCompatibleTo(proc.getShellDialect()))
                .toList();
        if (applicable.isEmpty()) {
            return null;
        }

        var refs = applicable.stream()
                .map(scriptStore -> {
                    return DataStorage.get().getStoreEntries().stream()
                            .filter(dataStoreEntry -> dataStoreEntry.getStore() == scriptStore)
                            .findFirst()
                            .map(entry -> entry.<SimpleScriptStore>ref());
                })
                .flatMap(Optional::stream)
                .toList();
        var hash = refs.stream()
                .mapToInt(value ->
                        value.get().getName().hashCode() + value.getStore().hashCode())
                .sum();
        var targetDir = ShellTemp.getUserSpecificTempDataDirectory(proc, "scripts")
                .join(proc.getShellDialect().getId())
                .toString();
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
                ErrorEvent.fromThrowable(e).expected().omit().handle();
            }
        }

        if (d.directoryExists(proc, targetDir).executeAndCheck()) {
            d.deleteFileOrDirectory(proc, targetDir).execute();
        }
        proc.executeSimpleCommand(d.getMkdirsCommand(targetDir));

        for (DataStoreEntryRef<SimpleScriptStore> scriptStore : refs) {
            var content = d.prepareScriptContent(scriptStore.getStore().getCommands());
            var fileName = proc.getOsType()
                    .makeFileSystemCompatible(
                            scriptStore.get().getName().toLowerCase(Locale.ROOT).replaceAll(" ", "_"));
            var scriptFile = FileNames.join(targetDir, fileName + "." + d.getScriptFileEnding());
            d.createScriptTextFileWriteCommand(proc, content, scriptFile).execute();
        }

        d.createTextFileWriteCommand(proc, String.valueOf(hash), hashFile).execute();
        return targetDir;
    }

    public static List<DataStoreEntryRef<ScriptStore>> getDefaultEnabledScripts() {
        return DataStorage.get().getStoreEntries().stream()
                .filter(dataStoreEntry -> dataStoreEntry.getStore() instanceof ScriptStore scriptStore
                        && scriptStore.getState().isEnabled())
                .map(DataStoreEntry::<ScriptStore>ref)
                .toList();
    }

    public static List<SimpleScriptStore> flatten(List<DataStoreEntryRef<ScriptStore>> scripts) {
        var seen = new LinkedHashSet<SimpleScriptStore>();
        scripts.forEach(scriptStoreDataStoreEntryRef ->
                scriptStoreDataStoreEntryRef.getStore().queryFlattenedScripts(seen));

        var dependencies = new HashMap<ScriptStore, Set<SimpleScriptStore>>();
        seen.forEach(simpleScriptStore -> {
            var f = new HashSet<>(simpleScriptStore.queryFlattenedScripts());
            f.remove(simpleScriptStore);
            dependencies.put(simpleScriptStore, f);
        });

        var sorted = new ArrayList<>(seen);
        sorted.sort((o1, o2) -> {
            if (dependencies.get(o1).contains(o2)) {
                return 1;
            }

            if (dependencies.get(o2).contains(o1)) {
                return -1;
            }

            return 0;
        });
        return sorted;
    }

    @Override
    public Class<EnabledStoreState> getStateClass() {
        return EnabledStoreState.class;
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.isType(group, ScriptGroupStore.class);
        if (scripts != null) {
            Validators.contentNonNull(scripts);
        }

        // Prevent possible stack overflow
        //        for (DataStoreEntryRef<ScriptStore> s : getEffectiveScripts()) {
        //         s.checkComplete();
        //        }
    }

    SequencedCollection<SimpleScriptStore> queryFlattenedScripts() {
        var seen = new LinkedHashSet<SimpleScriptStore>();
        queryFlattenedScripts(seen);
        return seen;
    }

    protected abstract void queryFlattenedScripts(LinkedHashSet<SimpleScriptStore> all);

    public abstract List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts();
}
