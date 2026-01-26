package io.xpipe.ext.base.script;

import io.xpipe.app.ext.StatefulDataStore;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.*;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.FilePath;
import lombok.SneakyThrows;

import java.util.*;

public class ScriptStoreSetup {

    public static void controlWithDefaultScripts(ShellControl pc) {
        controlWithScripts(pc, getEnabledScripts(), false);
    }

    public static void controlWithScripts(
            ShellControl pc, Collection<DataStoreEntryRef<ScriptStore>> enabledScripts, boolean append) {
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
            if (!append) {
                initFlattened = initFlattened.reversed();
            }
            var bringFlattened = flatten(enabledScripts).stream()
                    .filter(store -> store.getStore().isShellScript())
                    .filter(store -> finalDialect == null || store.getStore().isCompatible(finalDialect))
                    .toList();
            if (!append) {
                bringFlattened = bringFlattened.reversed();
            }

            // Optimize if we have nothing to do
            if (initFlattened.isEmpty() && bringFlattened.isEmpty()) {
                return;
            }

            initFlattened.forEach(s -> {
                pc.withInitSnippet(
                        new ShellTerminalInitCommand() {
                            @Override
                            public Optional<String> terminalContent(ShellControl shellControl) {
                                var assembled = s.getStore().assembleScriptChain(shellControl, false);
                                if (assembled == null) {
                                    return Optional.empty();
                                }

                                return Optional.ofNullable(assembled.getValue());
                            }

                            @Override
                            public boolean canPotentiallyRunInDialect(ShellDialect dialect) {
                                return s.getStore().isCompatible(dialect);
                            }
                        },
                        append);
            });
            if (!bringFlattened.isEmpty()) {
                var finalBringFlattened = bringFlattened;
                pc.withInitSnippet(
                        new ShellTerminalInitCommand() {

                            FilePath dir;

                            @Override
                            public Optional<String> terminalContent(ShellControl shellControl) throws Exception {
                                if (dir == null) {
                                    dir = initScriptsDirectory(shellControl, finalBringFlattened);
                                }

                                if (dir == null) {
                                    return Optional.empty();
                                }

                                return Optional.ofNullable(
                                        shellControl.getShellDialect().addToPathVariableCommand(List.of(dir.toString()), true));
                            }

                            @Override
                            public boolean canPotentiallyRunInDialect(ShellDialect dialect) {
                                return true;
                            }
                        },
                        append);
            }
        } catch (StackOverflowError t) {
            throw ErrorEventFactory.expected(
                    new RuntimeException("Unable to set up scripts. Is there a circular script dependency?", t));
        } catch (Throwable t) {
            throw new RuntimeException("Unable to set up scripts", t);
        }
    }

    @SneakyThrows
    private static FilePath initScriptsDirectory(ShellControl sc, List<DataStoreEntryRef<ScriptStore>> refs)
            throws Exception {
        if (refs.isEmpty()) {
            return null;
        }

        var applicable = refs.stream()
                .filter(ss -> ss.getStore().isCompatible(sc.getShellDialect()))
                .toList();
        if (applicable.isEmpty()) {
            return null;
        }

        var hash = refs.stream()
                .mapToInt(value ->
                        value.get().getName().hashCode() + value.getStore().hashCode())
                .sum();
        var targetDir = ShellTemp.createUserSpecificTempDataDirectory(sc, "scripts")
                .join(sc.getShellDialect().getId());
        var hashFile = targetDir.join("hash");
        if (sc.view().fileExists(hashFile)) {
            var read = sc.view().readTextFile(hashFile);
            try {
                var readHash = Integer.parseInt(read);
                if (hash == readHash) {
                    return targetDir;
                }
            } catch (NumberFormatException e) {
                ErrorEventFactory.fromThrowable(e).expected().omit().handle();
            }
        }

        if (sc.view().directoryExists(targetDir)) {
            sc.view().deleteDirectory(targetDir);
        }
        sc.view().mkdir(targetDir);

        for (DataStoreEntryRef<ScriptStore> ref : refs) {
            ref.getStore().getTextSource().checkAvailable();
        }

        var d = sc.getShellDialect();
        for (DataStoreEntryRef<ScriptStore> scriptStore : refs) {
            var src = scriptStore.getStore().getTextSource();
            var content = src.getText();
            var fileName = OsFileSystem.of(sc.getOsType())
                    .makeFileSystemCompatible(
                            scriptStore.get().getName().toLowerCase(Locale.ROOT).replaceAll(" ", "_"));
            var scriptFile = targetDir.join(fileName + "." + d.getScriptFileEnding());
            sc.view().writeScriptFile(scriptFile, content.getValue());
        }

        sc.view().writeTextFile(hashFile, String.valueOf(hash));
        return targetDir;
    }

    public static List<DataStoreEntryRef<ScriptStore>> getEnabledScripts() {
        var l = DataStorage.get().getStoreEntries().stream()
                .filter(dataStoreEntry -> dataStoreEntry.getValidity().isUsable()
                        && dataStoreEntry.getStore() instanceof ScriptStore ss
                        && ss.getState().isEnabled())
                .<DataStoreEntryRef<ScriptStore>>map(DataStoreEntry::ref)
                .toList();
        return l;
    }

    public static List<DataStoreEntryRef<ScriptStore>> flatten(Collection<DataStoreEntryRef<ScriptStore>> scripts) {
        var seen = new LinkedHashSet<DataStoreEntryRef<ScriptStore>>();
        scripts.stream()
                .filter(scriptStoreDataStoreEntryRef ->
                        scriptStoreDataStoreEntryRef.get().getValidity().isUsable())
                .forEach(scriptStoreDataStoreEntryRef ->
                        scriptStoreDataStoreEntryRef.getStore().queryFlattenedScripts(seen));

        var dependencies = new HashMap<DataStoreEntryRef<? extends ScriptStore>, Set<DataStoreEntryRef<ScriptStore>>>();
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
