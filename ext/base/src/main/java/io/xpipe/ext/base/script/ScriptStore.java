package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.*;
import io.xpipe.app.process.ScriptHelper;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

@SuperBuilder(toBuilder = true)
@Value
@Jacksonized
@JsonTypeName("script")
public class ScriptStore implements SelfReferentialStore, StatefulDataStore<EnabledStoreState>, ValidatableStore {

    @Singular
    List<DataStoreEntryRef<ScriptStore>> scripts;

    String description;

    ScriptTextSource textSource;
    boolean initScript;
    boolean shellScript;
    boolean fileScript;
    boolean runnableScript;



    @Override
    public Class<EnabledStoreState> getStateClass() {
        return EnabledStoreState.class;
    }

    SequencedCollection<DataStoreEntryRef<ScriptStore>> queryFlattenedScripts() {
        var seen = new LinkedHashSet<DataStoreEntryRef<ScriptStore>>();
        queryFlattenedScripts(seen);
        return seen;
    }

    public ShellDialect getShellDialect() {
        return textSource != null ? textSource.getDialect() : null;
    }

    public boolean isCompatible(ShellControl shellControl) {
        var targetType = shellControl.getOriginalShellDialect();
        return getShellDialect() == null || getShellDialect().isCompatibleTo(targetType);
    }

    public boolean isCompatible(ShellDialect dialect) {
        return getShellDialect() == null || getShellDialect().isCompatibleTo(dialect);
    }

    private String assembleScript(ShellControl shellControl, boolean args) {
        if (isCompatible(shellControl)) {
            var raw = getTextSource().getText().withoutShebang().getValue();
            if (raw.isBlank()) {
                return null;
            }

            var targetType = shellControl.getOriginalShellDialect();
            var scriptDialect = getShellDialect() != null ? getShellDialect() : targetType;
            var script = ScriptHelper.createExecScript(scriptDialect, shellControl, raw);
            var canSource = targetType.isSourceCompatibleTo(scriptDialect);
            var base = canSource
                    ? targetType.sourceScriptCommand(shellControl, script.toString())
                    : targetType.runScriptCommand(shellControl, script.toString());
            return base + (args ? " " + targetType.getCatchAllVariable() : "");
        }

        return null;
    }

    @SneakyThrows
    public ShellScript assembleScriptChain(ShellControl shellControl, boolean args) {
        var all = queryFlattenedScripts();

        for (DataStoreEntryRef<ScriptStore> ref : all) {
            ref.getStore().getTextSource().checkAvailable();
        }

        var r = all.stream()
                .map(ref -> ref.getStore().assembleScript(shellControl, args))
                .filter(s -> s != null)
                .toList();
        if (r.isEmpty()) {
            return null;
        }
        return ShellScript.lines(r);
    }

    public ShellScript assembleScriptForFile(ShellControl shellControl) {
        var raw = getTextSource().getText().withoutShebang().getValue();
        if (raw.isBlank()) {
            return null;
        }

        var targetType = shellControl.getOriginalShellDialect();
        var scriptDialect = getShellDialect() != null ? getShellDialect() : targetType;
        var content = scriptDialect.prepareScriptContent(shellControl, raw);
        return ShellScript.of(content);
    }

    @Override
    public void checkComplete() throws Throwable {
        if (textSource != null) {
            textSource.checkComplete();
        }
        if (!initScript && !shellScript && !fileScript && !runnableScript) {
            throw new ValidationException(AppI18n.get("valueMustNotBeEmpty"));
        }
        if (scripts != null) {
            Validators.contentNonNull(scripts);
            for (DataStoreEntryRef<ScriptStore> script : scripts) {
                Validators.nonNull(script);
                Validators.isType(script, ScriptStore.class);
            }
        }
    }

    public void queryFlattenedScripts(LinkedHashSet<DataStoreEntryRef<ScriptStore>> all) {
        DataStoreEntryRef<ScriptStore> ref = getSelfEntry().ref();
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

    public List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts() {
        return scripts != null
                ? scripts.stream()
                        .filter(Objects::nonNull)
                        .filter(ref -> ref.get().getValidity().isUsable())
                        .toList()
                : List.of();
    }

    public ScriptTextSource getTextSource() {
        return textSource != null
                ? textSource
                : ScriptTextSource.InPlace.builder().build();
    }

    @Override
    public void validate() throws Exception {
        getTextSource().validate();
    }
}
