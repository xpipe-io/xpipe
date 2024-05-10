package io.xpipe.ext.base.script;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellInitCommand;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@SuperBuilder
@Getter
@Jacksonized
@JsonTypeName("script")
public class SimpleScriptStore extends ScriptStore implements ShellInitCommand.Terminal {

    private final ShellDialect minimumDialect;
    private final String commands;

    private String assemble(ShellControl shellControl) {
        var targetType = shellControl.getOriginalShellDialect();
        if (minimumDialect.isCompatibleTo(targetType)) {
            var shebang = commands.startsWith("#");
            // Fix new lines and shebang
            var fixedCommands = commands.lines()
                    .skip(shebang ? 1 : 0)
                    .collect(Collectors.joining(
                            shellControl.getShellDialect().getNewLine().getNewLineString()));
            var script = ScriptHelper.createExecScript(targetType, shellControl, fixedCommands);
            return targetType.sourceScriptCommand(shellControl, script.toString());
        }

        return null;
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(group);
        super.checkComplete();
        Validators.nonNull(minimumDialect);
    }

    public void queryFlattenedScripts(LinkedHashSet<SimpleScriptStore> all) {
        // Prevent loop
        all.add(this);
        getEffectiveScripts().stream()
                .filter(scriptStoreDataStoreEntryRef -> !all.contains(scriptStoreDataStoreEntryRef.getStore()))
                .forEach(scriptStoreDataStoreEntryRef -> {
                    scriptStoreDataStoreEntryRef.getStore().queryFlattenedScripts(all);
                });
        all.remove(this);
        all.add(this);
    }

    @Override
    public List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts() {
        return scripts != null ? scripts.stream().filter(Objects::nonNull).toList() : List.of();
    }

    @Override
    public Optional<String> terminalContent(ShellControl shellControl) throws Exception {
        return Optional.ofNullable(assemble(shellControl));
    }
}
