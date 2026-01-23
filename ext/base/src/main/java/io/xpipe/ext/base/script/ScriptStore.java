package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.EnabledStoreState;
import io.xpipe.app.ext.SelfReferentialStore;
import io.xpipe.app.ext.StatefulDataStore;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.process.ScriptHelper;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialect;
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
public class ScriptStore implements SelfReferentialStore, StatefulDataStore<EnabledStoreState> {

    DataStoreEntryRef<ScriptGroupStore> group;
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

    public ShellDialect getMinimumDialect() {
        return textSource != null ? textSource.getDialect() : null;
    }

    public boolean isCompatible(ShellControl shellControl) {
        var targetType = shellControl.getOriginalShellDialect();
        return getMinimumDialect() == null || getMinimumDialect().isCompatibleTo(targetType);
    }

    public boolean isCompatible(ShellDialect dialect) {
        return getMinimumDialect() == null || getMinimumDialect().isCompatibleTo(dialect);
    }

    private String assembleScript(ShellControl shellControl, boolean args) {
        if (isCompatible(shellControl)) {
            var raw = getTextSource().getText().withoutShebang();
            var targetType = shellControl.getOriginalShellDialect();
            var script = ScriptHelper.createExecScript(targetType, shellControl, raw);
            return targetType.sourceScriptCommand(shellControl, script.toString()) + (args ? " "
                    + targetType.getCatchAllVariable() : "");
        }

        return null;
    }

    @SneakyThrows
    public String assembleScriptChain(ShellControl shellControl, boolean args) {
        var nl = shellControl.getShellDialect().getNewLine().getNewLineString();
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
        return String.join(nl, r);
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(textSource);
        textSource.checkComplete();
        Validators.nonNull(group);
        Validators.isType(group, ScriptGroupStore.class);
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
}
