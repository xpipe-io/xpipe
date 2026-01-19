package io.xpipe.ext.base.script;

import io.xpipe.app.process.ShellDialect;
import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;

@Value
@Builder
public class ScriptSourceEntry {

    String name;
    ShellDialect dialect;
    ScriptSource source;
    Path localFile;
}
