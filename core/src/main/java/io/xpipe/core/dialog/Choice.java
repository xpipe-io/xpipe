package io.xpipe.core.dialog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class Choice {
    Character character;
    String description;
    boolean disabled;

    public Choice(String description) {
        this.description = description;
        this.character = null;
        this.disabled = false;
    }

    public Choice(String description, boolean disabled) {
        this.character = null;
        this.description = description;
        this.disabled = disabled;
    }

    public Choice(Character character, String description) {
        this.character = character;
        this.description = description;
        this.disabled = false;
    }
}
