package io.xpipe.app.icon;

import lombok.Value;

import java.nio.file.Path;

@Value
public class SystemIconSourceFile {

    public enum ColorSchemeData {
        LIGHT,
        DARK,
        DEFAULT
    }

    SystemIconSource source;
    String name;
    Path file;
    ColorSchemeData colorSchemeData;
}
