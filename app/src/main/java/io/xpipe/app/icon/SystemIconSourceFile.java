package io.xpipe.app.icon;

import lombok.Value;

import java.nio.file.Path;

@Value
public class SystemIconSourceFile {

    SystemIconSource source;
    String name;
    Path file;
    boolean dark;
}
