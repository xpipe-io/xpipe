package io.xpipe.ext.base.desktop;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.ext.base.SelfReferentialStore;
import io.xpipe.ext.base.script.ScriptStore;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.stream.Stream;

@Getter
@SuperBuilder
@Jacksonized
@JsonTypeName("desktopEnvironment")
public class DesktopEnvironmentStore extends JacksonizedValue implements DesktopBaseStore, DataStore, SelfReferentialStore {

    private final DataStoreEntryRef<DesktopBaseStore> base;
    private final ExternalTerminalType terminal;
    private final ShellDialect dialect;
    private final List<DataStoreEntryRef<ScriptStore>> scripts;
    private final String initScript;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(base);
        Validators.isType(base, DesktopBaseStore.class);
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
        var filtered = f.stream().filter(simpleScriptStore -> simpleScriptStore.getMinimumDialect().isCompatibleTo(dialect) && simpleScriptStore.getExecutionType().runInTerminal()).toList();
        var initCommands = Stream.concat(filtered.stream().map(simpleScriptStore -> simpleScriptStore.getCommands()), command != null ? Stream.of(command) : Stream.of()).toList();
        var joined = String.join(dialect.getNewLine().getNewLineString(), initCommands);
        return !joined.isBlank() ? joined : null;
    }

    public void launch(String n, String commands) throws Exception {
        var fullName = n + " [" + getSelfEntry().getName() + "]";
        base.getStore().runDesktopScript(fullName, dialect, getMergedInitCommands(commands));
    }

    public void launchSelf() throws Exception {
        var fullName = getSelfEntry().getName();
        base.getStore().runDesktopTerminal(fullName, terminal, dialect, getMergedInitCommands(null));
    }

    @Override
    public boolean supportsDesktopAccess() {
        return base.getStore().supportsDesktopAccess();
    }

    @Override
    public void runDesktopScript(String name, ShellDialect dialect, String script) throws Exception {
        base.getStore().runDesktopScript(name, dialect, script);
    }

    @Override
    public void runDesktopTerminal(String name, ExternalTerminalType terminalType, ShellDialect dialect, String script) throws Exception {
        base.getStore().runDesktopTerminal(name,terminalType,dialect,script);
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
