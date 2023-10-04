package io.xpipe.ext.base.script;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;


@SuperBuilder
@Getter
@Jacksonized
@JsonTypeName("script")
public class SimpleScriptStore extends ScriptStore {

    @Override
    public String prepareDumbScript(ShellControl shellControl) {
        return assemble(shellControl, ExecutionType.DUMB_ONLY, ScriptStore::prepareDumbScript);
    }

    @Override
    public String prepareTerminalScript(ShellControl shellControl) {
        return assemble(shellControl, ExecutionType.TERMINAL_ONLY, ScriptStore::prepareTerminalScript);
    }

    private String assemble(ShellControl shellControl, ExecutionType type, BiFunction<ScriptStore, ShellControl, String> function) {
        var list = new ArrayList<String>();
        scripts.forEach(scriptStoreDataStoreEntryRef -> {
            var s = function.apply(scriptStoreDataStoreEntryRef.getStore(), shellControl);
            if (s != null) {
                list.add(s);
            }
        });

        if ((executionType == type || executionType == ExecutionType.BOTH) && minimumDialect.isCompatibleTo(shellControl.getShellDialect())) {
            var script = ScriptHelper.createExecScript(minimumDialect, shellControl, commands);
            list.add(shellControl.getShellDialect().sourceScriptCommand(shellControl, script));
        }

        var cmd = String.join("\n", list);
        return cmd.isEmpty() ? null : cmd;
    }

    @Override
    public List<DataStoreEntryRef<ScriptStore>> getEffectiveScripts() {
        return scripts != null ? scripts.stream().filter(scriptStore -> scriptStore != null).toList() : List.of();
    }

    @Getter
    public static enum ExecutionType {
        @JsonProperty("dumbOnly")
        DUMB_ONLY("dumbOnly"),
        @JsonProperty("terminalOnly")
        TERMINAL_ONLY("terminalOnly"),
        @JsonProperty("both")
        BOTH("both");

        private final String id;

        ExecutionType(String id) {
            this.id = id;
        }
    }

    private final ShellDialect minimumDialect;
    private final String commands;
    private final ExecutionType executionType;
    private final boolean requiresElevation;

    @Override
    public void checkComplete() throws Exception {
        super.checkComplete();
        Validators.nonNull(executionType);
        Validators.nonNull(minimumDialect);
    }
}
