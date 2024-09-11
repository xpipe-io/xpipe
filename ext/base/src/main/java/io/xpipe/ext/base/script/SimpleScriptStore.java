package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellInitCommand;
import io.xpipe.core.util.ValidationException;
import io.xpipe.ext.base.SelfReferentialStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@SuperBuilder(toBuilder = true)
@Getter
@Jacksonized
@JsonTypeName("script")
public class SimpleScriptStore extends ScriptStore implements ShellInitCommand.Terminal, SelfReferentialStore {

    private final ShellDialect minimumDialect;
    private final String commands;
    private final boolean initScript;
    private final boolean shellScript;
    private final boolean fileScript;
    private final boolean runnableScript;

    public boolean isCompatible(ShellControl shellControl) {
        var targetType = shellControl.getOriginalShellDialect();
        return minimumDialect.isCompatibleTo(targetType);
    }

    public boolean isCompatible(ShellDialect dialect) {
        return minimumDialect.isCompatibleTo(dialect);
    }

    private String assembleScript(ShellControl shellControl) {
        if (isCompatible(shellControl)) {
            var shebang = commands.startsWith("#");
            // Fix new lines and shebang
            var fixedCommands = commands.lines()
                    .skip(shebang ? 1 : 0)
                    .collect(Collectors.joining(
                            shellControl.getShellDialect().getNewLine().getNewLineString()));
            var targetType = shellControl.getOriginalShellDialect();
            var script = ScriptHelper.createExecScript(targetType, shellControl, fixedCommands);
            return targetType.sourceScriptCommand(shellControl, script.toString());
        }

        return null;
    }

    public String assembleScriptChain(ShellControl shellControl) {
        var nl = shellControl.getShellDialect().getNewLine().getNewLineString();
        var all = queryFlattenedScripts();
        var r = all.stream()
                .map(ref -> ref.getStore().assembleScript(shellControl))
                .filter(s -> s != null)
                .toList();
        if (r.isEmpty()) {
            return null;
        }
        return String.join(nl, r);
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(group);
        super.checkComplete();
        Validators.nonNull(minimumDialect);
        if (!initScript && !shellScript && !fileScript && !isRunnableScript()) {
            throw new ValidationException(AppI18n.get("app.valueMustNotBeEmpty"));
        }
    }

    public void queryFlattenedScripts(LinkedHashSet<DataStoreEntryRef<SimpleScriptStore>> all) {
        // Prevent loop
        DataStoreEntryRef<SimpleScriptStore> ref = getSelfEntry().ref();
        all.add(ref);
        getEffectiveScripts().stream()
                .filter(scriptStoreDataStoreEntryRef -> !all.contains(scriptStoreDataStoreEntryRef))
                .forEach(scriptStoreDataStoreEntryRef -> {
                    scriptStoreDataStoreEntryRef.getStore().queryFlattenedScripts(all);
                });
        all.remove(ref);
        all.add(ref);
    }

    @Override
    public List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts() {
        return scripts != null
                ? scripts.stream()
                        .filter(Objects::nonNull)
                        .filter(ref -> ref.get().getValidity().isUsable())
                        .toList()
                : List.of();
    }

    @Override
    public Optional<String> terminalContent(ShellControl shellControl) {
        return Optional.ofNullable(assembleScriptChain(shellControl));
    }
}
