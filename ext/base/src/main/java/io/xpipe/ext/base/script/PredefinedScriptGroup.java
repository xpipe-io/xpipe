package io.xpipe.ext.base.script;

import io.xpipe.app.storage.DataStoreEntryRef;
import lombok.Getter;
import lombok.Setter;

@Getter
public enum PredefinedScriptGroup {
    CLINK("Clink", null),
    STARSHIP("Starship", "Scripts to enable the starship shell extension");

    private final String name;
    private final String description;

    @Setter
    private DataStoreEntryRef<ScriptGroupStore> entry;

    PredefinedScriptGroup(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
