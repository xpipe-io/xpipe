package io.xpipe.core.dialog;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

/**
 * A reference to a dialogue instance that will be exchanged whenever a dialogue is started.
 */
@Value
public class DialogReference {

    @NonNull UUID dialogId;

    DialogElement start;

    @JsonCreator
    public DialogReference(@NonNull UUID dialogId, DialogElement start) {
        this.dialogId = dialogId;
        this.start = start;
    }
}
