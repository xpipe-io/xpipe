package io.xpipe.app.core;

import io.xpipe.core.OsType;

import java.nio.file.Files;
import java.nio.file.Path;

public class AppSystemInfo {

    public static class Windows {

    }

    public static Linux linux() {
        if (OsType.getLocal() != OsType.LINUX) {
            throw new IllegalStateException();
        }

        return new Linux();
    }

    public static class Linux {

        public boolean isDebianBased() {
            return Files.exists(Path.of("/etc/debian_version"));
        }
    }

    public static class MacOS {

    }
}
