package io.xpipe.core.dialog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class DialogReference {

    UUID dialogId;
    DialogElement start;
}
