package io.xpipe.ext.base.script;

import io.xpipe.app.storage.DataStoreEntryRef;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum PredefinedScriptGroup {
    CLINK("Clink", null, false),
    STARSHIP("Starship", "Sets up and enables the starship shell prompt", true);

    private final String name;
    private final String description;
    private final boolean expanded;

    @Setter
    private DataStoreEntryRef<ScriptGroupStore> entry;

    PredefinedScriptGroup(String name, String description, boolean expanded) {
        this.name = name;
        this.description = description;
        this.expanded = expanded;
    }
}
