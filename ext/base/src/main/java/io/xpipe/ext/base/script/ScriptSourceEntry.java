package io.xpipe.ext.base.script;

import io.xpipe.app.process.ShellDialect;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;

@Value
@Builder
@Jacksonized
public class ScriptSourceEntry {

    String name;
    ShellDialect dialect;
    ScriptSource source;
    Path localFile;
}
