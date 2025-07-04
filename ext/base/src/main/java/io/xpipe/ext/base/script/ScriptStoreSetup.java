package io.xpipe.ext.base.script;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.*;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ShellTemp;

import io.xpipe.core.FilePath;
import io.xpipe.app.ext.StatefulDataStore;

import java.util.*;

public class ScriptStoreSetup {

    public static void controlWithDefaultScripts(ShellControl pc) {
        controlWithScripts(pc, getEnabledScripts());
    }

    public static void controlWithScripts(ShellControl pc, List<DataStoreEntryRef<ScriptStore>> enabledScripts) {
        try {
            var dialect = pc.getShellDialect();
            if (dialect == null) {
                var source = pc.getSourceStore();
                if (source.isPresent() && source.get() instanceof StatefulDataStore<?> sds) {
                    var state = sds.getState();
                    if (state instanceof SystemState systemState) {
                        dialect = systemState.getShellDialect();
                    }
                }
            }

            var finalDialect = dialect;
            var initFlattened = flatten(enabledScripts).stream()
                    .filter(store -> store.getStore().isInitScript())
                    .filter(store -> finalDialect == null || store.getStore().isCompatible(finalDialect))
                    .toList();
            var bringFlattened = flatten(enabledScripts).stream()
                    .filter(store -> store.getStore().isShellScript())
                    .filter(store -> finalDialect == null || store.getStore().isCompatible(finalDialect))
                    .toList();

            // Optimize if we have nothing to do
            if (initFlattened.isEmpty() && bringFlattened.isEmpty()) {
                return;
            }

            initFlattened.forEach(s -> {
                pc.withInitSnippet(new ShellTerminalInitCommand() {
                    @Override
                    public Optional<String> terminalContent(ShellControl shellControl) {
                        return Optional.ofNullable(s.getStore().assembleScriptChain(shellControl));
                    }

                    @Override
                    public boolean canPotentiallyRunInDialect(ShellDialect dialect) {
                        return s.getStore().isCompatible(dialect);
                    }
                });
            });
            if (!bringFlattened.isEmpty()) {
                pc.withInitSnippet(new ShellTerminalInitCommand() {

                    String dir;

                    @Override
                    public Optional<String> terminalContent(ShellControl shellControl) throws Exception {
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
                });
            }
        } catch (StackOverflowError t) {
            throw ErrorEventFactory.expected(
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
                .filter(simpleScriptStore -> simpleScriptStore.getStore().isCompatible(proc.getShellDialect()))
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
        var hashFile = FilePath.of(targetDir, "hash");
        var d = proc.getShellDialect();
        if (d.createFileExistsCommand(proc, hashFile.toString()).executeAndCheck()) {
            var read = d.getFileReadCommand(proc, hashFile.toString()).readStdoutOrThrow();
            try {
                var readHash = Integer.parseInt(read);
                if (hash == readHash) {
                    return targetDir;
                }
            } catch (NumberFormatException e) {
                ErrorEventFactory.fromThrowable(e).expected().omit().handle();
            }
        }

        if (d.directoryExists(proc, targetDir).executeAndCheck()) {
            d.deleteFileOrDirectory(proc, targetDir).execute();
        }
        proc.executeSimpleCommand(d.getMkdirsCommand(targetDir));

        for (DataStoreEntryRef<SimpleScriptStore> scriptStore : refs) {
            var content = d.prepareScriptContent(proc, scriptStore.getStore().getCommands());
            var fileName = OsFileSystem.of(proc.getOsType())
                    .makeFileSystemCompatible(
                            scriptStore.get().getName().toLowerCase(Locale.ROOT).replaceAll(" ", "_"));
            var scriptFile = FilePath.of(targetDir, fileName + "." + d.getScriptFileEnding());
            proc.view().writeScriptFile(scriptFile, content);
        }

        proc.view().writeTextFile(hashFile, String.valueOf(hash));
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
