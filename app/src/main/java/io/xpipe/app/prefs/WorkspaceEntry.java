package io.xpipe.app.prefs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;

@Value
@Builder
@Jacksonized
public class WorkspaceEntry {

    String name;
    Path dir;

    @JsonIgnore
    @NonFinal
    @Setter
    @EqualsAndHashCode.Exclude
    @Builder.Default
    boolean available = true;
}
