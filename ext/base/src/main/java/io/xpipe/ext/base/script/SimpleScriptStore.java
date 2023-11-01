package io.xpipe.ext.base.script;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.ScriptSnippet;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@SuperBuilder
@Getter
@Jacksonized
@JsonTypeName("script")
public class SimpleScriptStore extends ScriptStore implements ScriptSnippet {

    private String assemble(ShellControl shellControl, ExecutionType type) {
        var targetType = type == ExecutionType.TERMINAL_ONLY
                ? shellControl.getTargetTerminalShellDialect()
                : shellControl.getShellDialect();
        if ((executionType == type || executionType == ExecutionType.BOTH)
                && minimumDialect.isCompatibleTo(targetType)) {
            var shebang = commands.startsWith("#");
            // Fix new lines and shebang
            var fixedCommands = commands.lines()
                    .skip(shebang ? 1 : 0)
                    .collect(Collectors.joining(
                            shellControl.getShellDialect().getNewLine().getNewLineString()));
            var script = ScriptHelper.createExecScript(targetType, shellControl, fixedCommands);
            return targetType.sourceScriptCommand(shellControl, script);
        }

        return null;
    }

    @Override
    public List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts() {
        return scripts != null
                ? scripts.stream().filter(scriptStore -> scriptStore != null).toList()
                : List.of();
    }

    public void queryFlattenedScripts(LinkedHashSet<SimpleScriptStore> all) {
        all.add(this);
        getEffectiveScripts().stream()
                .filter(scriptStoreDataStoreEntryRef -> !all.contains(scriptStoreDataStoreEntryRef.getStore()))
                .forEach(scriptStoreDataStoreEntryRef -> {
                    scriptStoreDataStoreEntryRef.getStore().queryFlattenedScripts(all);
                });
    }

    @Override
    public String content(ShellControl shellControl) {
        return assemble(shellControl, executionType);
    }

    @Override
    public ScriptSnippet.ExecutionType executionType() {
        return executionType;
    }

    private final ShellDialect minimumDialect;
    private final String commands;
    private final ExecutionType executionType;
    private final boolean requiresElevation;

    @Override
    public void checkComplete() throws Exception {
        Validators.nonNull(group);
        super.checkComplete();
        Validators.nonNull(executionType);
        Validators.nonNull(minimumDialect);
    }
}
