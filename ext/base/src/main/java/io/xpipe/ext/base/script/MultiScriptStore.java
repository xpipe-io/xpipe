package io.xpipe.ext.base.script;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.ShellControl;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Objects;

@SuperBuilder
@Getter
@Jacksonized
@JsonTypeName("multiScript")
public class MultiScriptStore extends ScriptStore {

    @Override
    public String prepareDumbScript(ShellControl shellControl) {
        return getEffectiveScripts().stream().map(scriptStore -> {
            return ((ScriptStore) scriptStore.getStore()).prepareDumbScript(shellControl);
        }).filter(
                Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public String prepareTerminalScript(ShellControl shellControl) {
        return getEffectiveScripts().stream().map(scriptStore -> {
            return ((ScriptStore) scriptStore.getStore()).prepareDumbScript(shellControl);
        }).filter(
                Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public void checkComplete() throws Exception {
        if (scripts != null) {
            Validators.contentNonNull(scripts);
            for (var script : scripts) {
                script.getStore().checkComplete();
            }
        }
    }

    @Override
    public List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts() {
        return scripts != null ? scripts.stream().filter(scriptStore -> scriptStore != null).toList() : List.of();
    }
}
