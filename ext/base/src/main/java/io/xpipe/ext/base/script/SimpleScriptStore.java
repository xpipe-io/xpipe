package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.SelfReferentialStore;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.Validators;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SuperBuilder(toBuilder = true)
@Value
@Jacksonized
@JsonTypeName("script")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SimpleScriptStore extends ScriptStore implements SelfReferentialStore {

    ShellDialect minimumDialect;
    String commands;
    boolean initScript;
    boolean shellScript;
    boolean fileScript;
    boolean runnableScript;

    public String getCommands() {
        return commands != null ? commands : "";
    }

    public boolean isCompatible(ShellControl shellControl) {
        var targetType = shellControl.getOriginalShellDialect();
        return minimumDialect == null || minimumDialect.isCompatibleTo(targetType);
    }

    public boolean isCompatible(ShellDialect dialect) {
        return minimumDialect == null || minimumDialect.isCompatibleTo(dialect);
    }

    private String assembleScript(ShellControl shellControl) {
        if (isCompatible(shellControl)) {
            var shebang = getCommands().startsWith("#");
            // Fix new lines and shebang
            var fixedCommands = getCommands()
                    .lines()
                    .skip(shebang ? 1 : 0)
                    .collect(Collectors.joining(
                            shellControl.getShellDialect().getNewLine().getNewLineString()));
            var targetType = shellControl.getOriginalShellDialect();
            var script = ScriptHelper.createExecScript(targetType, shellControl, fixedCommands);
            return targetType.sourceScriptCommand(script.toString());
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
        if (!initScript && !shellScript && !fileScript && !runnableScript) {
            throw new ValidationException(AppI18n.get("app.valueMustNotBeEmpty"));
        }
    }

    public void queryFlattenedScripts(LinkedHashSet<DataStoreEntryRef<SimpleScriptStore>> all) {
        DataStoreEntryRef<SimpleScriptStore> ref = getSelfEntry().ref();
        var added = all.add(ref);
        // Prevent loop
        if (added) {
            getEffectiveScripts().stream()
                    .filter(scriptStoreDataStoreEntryRef -> !all.contains(scriptStoreDataStoreEntryRef))
                    .forEach(scriptStoreDataStoreEntryRef -> {
                        scriptStoreDataStoreEntryRef.getStore().queryFlattenedScripts(all);
                    });
            all.remove(ref);
            all.add(ref);
        }
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
}
