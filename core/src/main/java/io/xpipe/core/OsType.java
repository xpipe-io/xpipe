package io.xpipe.core;

import java.util.Locale;

public interface OsType {

    Windows WINDOWS = new Windows();
    Linux LINUX = new Linux();
    MacOs MACOS = new MacOs();
    Bsd BSD = new Bsd();
    Solaris SOLARIS = new Solaris();
    Aix AIX = new Aix();

    static Local ofLocal() {
        String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((osName.contains("mac")) || (osName.contains("darwin"))) {
            return MACOS;
        } else if (osName.contains("win")) {
            return WINDOWS;
        } else {
            return LINUX;
        }
    }

    String getId();

    String getName();

    sealed interface Local extends OsType permits OsType.Windows, OsType.Linux, OsType.MacOs {

        default Any toAny() {
            return (Any) this;
        }
    }

    sealed interface Any extends OsType
            permits OsType.Windows, OsType.Linux, OsType.MacOs, OsType.Solaris, OsType.Bsd, OsType.Aix {}

    final class Windows implements OsType, Local, Any {

        @Override
        public String getName() {
            return "Windows";
        }

        @Override
        public String getId() {
            return "windows";
        }
    }

    final class Linux implements OsType, Local, Any {

        @Override
        public String getName() {
            return "Linux";
        }

        @Override
        public String getId() {
            return "linux";
        }
    }

    final class Solaris implements Any {

        @Override
        public String getId() {
            return "solaris";
        }

        @Override
        public String getName() {
            return "Solaris";
        }
    }

    final class Aix implements Any {

        @Override
        public String getId() {
            return "aix";
        }

        @Override
        public String getName() {
            return "AIX";
        }
    }

    final class Bsd implements Any {

        @Override
        public String getId() {
            return "bsd";
        }

        @Override
        public String getName() {
            return "Bsd";
        }
    }

    final class MacOs implements OsType, Local, Any {

        @Override
        public String getId() {
            return "macos";
        }

        @Override
        public String getName() {
            return "Mac";
        }
    }
}
