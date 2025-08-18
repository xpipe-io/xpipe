package io.xpipe.core;

import java.util.Locale;

public interface OsType {

    Windows WINDOWS = new Windows();
    Linux LINUX = new Linux();
    MacOs MACOS = new MacOs();
    Bsd BSD = new Bsd();
    Solaris SOLARIS = new Solaris();

    static Local getLocal() {
        String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((osName.contains("mac")) || (osName.contains("darwin"))) {
            return MACOS;
        } else if (osName.contains("win")) {
            return WINDOWS;
        } else {
            return LINUX;
        }
    }

    String getName();

    sealed interface Local extends OsType permits OsType.Windows, OsType.Linux, OsType.MacOs {

        String getId();

        default Any toAny() {
            return (Any) this;
        }
    }

    sealed interface Any extends OsType
            permits OsType.Windows, OsType.Linux, OsType.MacOs, OsType.Solaris, OsType.Bsd {}

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

    abstract class Unix implements OsType {

    }

    final class Linux extends Unix implements OsType, Local, Any {

        @Override
        public String getName() {
            return "Linux";
        }
        
        @Override
        public String getId() {
            return "linux";
        }
    }

    final class Solaris extends Unix implements Any {

        @Override
        public String getName() {
            return "Solaris";
        }
    }

    final class Bsd extends Unix implements Any {

        @Override
        public String getName() {
            return "Bsd";
        }}

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
