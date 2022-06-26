package io.xpipe.core.dialog;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

@Value
public class DialogReference {

    @NonNull
    UUID dialogId;

    DialogElement start;

    @JsonCreator
    public DialogReference(UUID dialogId, DialogElement start) {
        this.dialogId = dialogId;
        this.start = start;
    }
}
