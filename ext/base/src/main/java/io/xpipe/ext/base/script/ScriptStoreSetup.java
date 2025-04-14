package io.xpipe.ext.base.script;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.FailableFunction;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class ScriptStoreSetup {

    public static void controlWithDefaultScripts(ShellControl pc) {
        controlWithScripts(pc, getEnabledScripts());
    }

    public static void controlWithScripts(ShellControl pc, List<DataStoreEntryRef<ScriptStore>> enabledScripts) {
        try {
            // Don't copy scripts if we don't want to modify the file system
            if (!pc.getEffectiveSecurityPolicy().permitTempScriptCreation()) {
                return;
            }

            // If we don't have write permissions / it is a read-only file system, don't create scripts
            if (pc.getOsType() == OsType.LINUX) {
                var test = pc.command(CommandBuilder.of().add("test", "-w").addFile(pc.getSystemTemporaryDirectory())).executeAndCheck();
                if (!test) {
                    return;
                }
            }

            var checkedPermissions = new AtomicReference<Boolean>();
            FailableFunction<ShellControl, Boolean, Exception> permissionCheck = (sc) -> {
                if (checkedPermissions.get() != null) {
                    return checkedPermissions.get();
                }

                // If we don't have write permissions / it is a read-only file system, don't create scripts
                if (sc.getOsType() == OsType.LINUX) {
                    var file = sc.getSystemTemporaryDirectory().join("xpipe-test");
                    var test = sc.command(CommandBuilder.of().add("touch").addFile(file).add("&&", "rm").addFile(file)).executeAndCheck();
                    if (!test) {
                        checkedPermissions.set(false);
                        return false;
                    }
                }

                checkedPermissions.set(true);
                return true;
            };

            var initFlattened = flatten(enabledScripts).stream()
                    .filter(store -> store.getStore().isInitScript())
                    .toList();
            var bringFlattened = flatten(enabledScripts).stream()
                    .filter(store -> store.getStore().isShellScript())
                    .toList();

            // Optimize if we have nothing to do
            if (initFlattened.isEmpty() && bringFlattened.isEmpty()) {
                return;
            }

            initFlattened.forEach(s -> {
                pc.withInitSnippet(new ShellTerminalInitCommand() {
                    @Override
                    public Optional<String> terminalContent(ShellControl shellControl) throws Exception {
                        if (!permissionCheck.apply(shellControl)) {
                            return Optional.empty();
                        }

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
                        if (!permissionCheck.apply(shellControl)) {
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
                });
            }
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
