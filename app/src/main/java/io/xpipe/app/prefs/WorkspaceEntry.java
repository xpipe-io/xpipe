package io.xpipe.app.prefs;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;

@Value
@Builder
@Jacksonized
public class WorkspaceEntry {

    String name;
    Path dir;
}
