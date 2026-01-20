package io.xpipe.ext.base.script;

import io.xpipe.app.process.ShellDialect;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;

@Value
@Builder
@Jacksonized
public class ScriptCollectionSourceEntry {

    String name;
    ShellDialect dialect;
    ScriptCollectionSource source;
    Path localFile;
}
