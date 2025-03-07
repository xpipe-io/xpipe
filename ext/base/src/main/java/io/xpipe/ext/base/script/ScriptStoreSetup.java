package io.xpipe.ext.base.script;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellInitCommand;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FilePath;
import lombok.Value;

import java.util.*;
import java.util.stream.Collectors;

public class ScriptStoreSetup {

    @Value
    private static class GeneratedScriptCache {

        UUID storeId;
        List<Integer> scriptStoreHashes;
    }

    private static final Set<GeneratedScriptCache> generatedScriptCaches = new HashSet<>();

    private static synchronized boolean hasGeneratedScriptsCached(UUID storeId, List<DataStoreEntryRef<SimpleScriptStore>> scripts) {
        var c = generatedScriptCaches.stream().filter(e -> e.storeId.equals(storeId)).findFirst();
        if (c.isEmpty()) {
            return false;
        }

        return scripts.stream().allMatch(ref -> c.get().getScriptStoreHashes().contains(ref.getStore().hashCode()));
    }

    private static synchronized void cacheScripts(UUID storeId, List<DataStoreEntryRef<SimpleScriptStore>> scripts) {
        var c = generatedScriptCaches.stream().filter(e -> e.storeId.equals(storeId)).findFirst();
        if (c.isEmpty()) {
            var newC = new GeneratedScriptCache(storeId, new ArrayList<>());
            generatedScriptCaches.add(newC);
            c = Optional.of(newC);
        }
        c.get().getScriptStoreHashes().addAll(scripts.stream().map(ref -> ref.getStore().hashCode()).toList());
    }

    public static ShellControl controlWithDefaultScripts(ShellControl pc) {
        return controlWithScripts(pc, getEnabledScripts());
    }

    public static ShellControl controlWithScripts(
            ShellControl pc, List<DataStoreEntryRef<ScriptStore>> enabledScripts) {
        try {
            // Don't copy scripts if we don't want to modify the file system
            if (!pc.getEffectiveSecurityPolicy().permitTempScriptCreation()) {
                return pc;
            }

            var initFlattened = flatten(enabledScripts).stream()
                    .filter(store -> store.getStore().isInitScript())
                    .toList();
            var bringFlattened = flatten(enabledScripts).stream()
                    .filter(store -> store.getStore().isShellScript())
                    .toList();

            // Optimize if we have nothing to do
            if (initFlattened.isEmpty() && bringFlattened.isEmpty()) {
                return pc;
            }

            var source = pc.getSourceStoreId();
            if (source.isPresent()) {
                var useCached = pc.getSourceStore().map(dataStore -> dataStore instanceof ShellStore s && s.isConnectionAttemptCostly()).orElse(false);
                if (useCached) {
                    var cached = hasGeneratedScriptsCached(source.get(), initFlattened) &&  hasGeneratedScriptsCached(source.get(), bringFlattened);
                    if (cached) {
                        return pc;
                    }
                }
            }

            initFlattened.forEach(simpleScriptStore -> {
                pc.withInitSnippet(simpleScriptStore.getStore());
            });
            if (!bringFlattened.isEmpty() || source.isPresent()) {
                pc.withInitSnippet(new ShellInitCommand() {

                    String dir;

                    @Override
                    public Optional<String> terminalContent(ShellControl shellControl) throws Exception {
                        if (source.isPresent()) {
                            cacheScripts(source.get(), initFlattened);
                            cacheScripts(source.get(), bringFlattened);
                        }

                        if (bringFlattened.isEmpty()) {
                            return Optional.empty();
                        }

                        if (dir == null) {
                            dir = initScriptsDirectory(shellControl, bringFlattened);
                        }

                        if (dir == null) {
                            return Optional.empty();
                        }

                        return Optional.ofNullable(
                                shellControl.getShellDialect().addToPathVariableCommand(List.of(dir), true));
                    }

                    @Override
                    public boolean canPotentiallyRunInDialect(ShellDialect dialect) {
                        return true;
                    }

                    @Override
                    public boolean runInTerminal() {
                        return true;
                    }
                });
            }
            return pc;
        } catch (StackOverflowError t) {
            throw ErrorEvent.expected(
                    new RuntimeException("Unable to set up scripts. Is there a circular script dependency?", t));
        } catch (Throwable t) {
            throw new RuntimeException("Unable to set up scripts", t);
        }
    }

    private static String initScriptsDirectory(ShellControl proc, List<DataStoreEntryRef<SimpleScriptStore>> refs)
            throws Exception {
        if (refs.isEmpty()) {
            return null;
        }

        var applicable = refs.stream()
                .filter(simpleScriptStore ->
                        simpleScriptStore.getStore().getMinimumDialect().isCompatibleTo(proc.getShellDialect()))
                .toList();
        if (applicable.isEmpty()) {
            return null;
        }

        var hash = refs.stream()
                .mapToInt(value ->
                        value.get().getName().hashCode() + value.getStore().hashCode())
                .sum();
        var targetDir = ShellTemp.createUserSpecificTempDataDirectory(proc, "scripts")
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
            proc.view().writeScriptFile(FilePath.of(scriptFile), content);
        }

        proc.view().writeTextFile(FilePath.of(hashFile), String.valueOf(hash));
        return targetDir;
    }

    public static List<DataStoreEntryRef<ScriptStore>> getEnabledScripts() {
        return DataStorage.get().getStoreEntries().stream()
                .filter(dataStoreEntry -> dataStoreEntry.getValidity().isUsable()
                        && dataStoreEntry.getStore() instanceof ScriptStore scriptStore
                        && scriptStore.getState().isEnabled())
                .map(DataStoreEntry::<ScriptStore>ref)
                .toList();
    }

    public static List<DataStoreEntryRef<SimpleScriptStore>> flatten(List<DataStoreEntryRef<ScriptStore>> scripts) {
        var seen = new LinkedHashSet<DataStoreEntryRef<SimpleScriptStore>>();
        scripts.stream()
                .filter(scriptStoreDataStoreEntryRef ->
                        scriptStoreDataStoreEntryRef.get().getValidity().isUsable())
                .forEach(scriptStoreDataStoreEntryRef ->
                        scriptStoreDataStoreEntryRef.getStore().queryFlattenedScripts(seen));

        var dependencies =
                new HashMap<DataStoreEntryRef<? extends ScriptStore>, Set<DataStoreEntryRef<SimpleScriptStore>>>();
        seen.forEach(ref -> {
            var f = new HashSet<>(ref.getStore().queryFlattenedScripts());
            f.remove(ref);
            dependencies.put(ref, f);
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
}
