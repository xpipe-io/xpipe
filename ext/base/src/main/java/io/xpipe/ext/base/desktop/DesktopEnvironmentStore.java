package io.xpipe.ext.base.desktop;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.ext.base.SelfReferentialStore;
import io.xpipe.ext.base.script.ScriptStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
@Jacksonized
@JsonTypeName("desktopEnvironment")
public class DesktopEnvironmentStore extends JacksonizedValue
        implements DesktopBaseStore, DataStore, SelfReferentialStore {

    private final DataStoreEntryRef<DesktopBaseStore> base;
    private final ExternalTerminalType terminal;
    private final ShellDialect dialect;
    private final List<DataStoreEntryRef<ScriptStore>> scripts;
    private final String initScript;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(base);
        Validators.isType(base, DesktopBaseStore.class);
        base.checkComplete();
        Validators.nonNull(terminal);
        Validators.nonNull(dialect);
    }

    public List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts() {
        return scripts != null
                ? scripts.stream().filter(scriptStore -> scriptStore != null).toList()
                : List.of();
    }

    public String getMergedInitCommands(String command) {
        var f = ScriptStore.flatten(scripts);
        var filtered = f.stream()
                .filter(simpleScriptStore ->
                        simpleScriptStore.getStore().getMinimumDialect().isCompatibleTo(dialect))
                .toList();
        var initCommands = new ArrayList<>(filtered.stream()
                .map(simpleScriptStore -> simpleScriptStore.getStore().getCommands())
                .toList());
        if (initScript != null) {
            initCommands.add(initScript);
        }
        if (command != null) {
            initCommands.add(command);
        }
        var joined = String.join(dialect.getNewLine().getNewLineString(), initCommands);
        return joined;
    }

    @Override
    public boolean supportsDesktopAccess() {
        return base.getStore().supportsDesktopAccess();
    }

    @Override
    public void runDesktopApplication(String name, DesktopApplicationStore applicationStore) throws Exception {
        var fullName = name + " [" + getSelfEntry().getName() + "]";
        base.getStore().runDesktopApplication(fullName, applicationStore);
    }

    @Override
    public void runDesktopScript(String name, String script) throws Exception {
        var fullName = getSelfEntry().getName();
        base.getStore().runDesktopScript(fullName, getMergedInitCommands(script));
    }

    @Override
    public FilePath createScript(ShellDialect dialect, String content) throws Exception {
        return base.getStore().createScript(dialect, content);
    }

    public void runDesktopTerminal(String name, String script) throws Exception {
        var launchCommand = terminal.remoteLaunchCommand(base.getStore().getUsedDialect());
        var toExecute = (script != null
                ? getMergedInitCommands(
                        script + "\n" + dialect.getPauseCommand() + "\n" + dialect.getNormalExitCommand())
                : getMergedInitCommands(null));
        var scriptFile = base.getStore().createScript(dialect, toExecute);
        var launchScriptFile = base.getStore()
                .createScript(
                        dialect,
                        dialect.prepareTerminalInitFileOpenCommand(dialect, null, scriptFile.toString(), false));
        var launchConfig = new ExternalTerminalType.LaunchConfiguration(null, name, name, launchScriptFile, dialect);
        base.getStore().runDesktopScript(name, launchCommand.apply(launchConfig));
    }

    @Override
    public ShellDialect getUsedDialect() {
        return dialect;
    }

    @Override
    public OsType getUsedOsType() {
        return base != null ? base.getStore().getUsedOsType() : null;
    }
}
