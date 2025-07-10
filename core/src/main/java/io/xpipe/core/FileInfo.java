package io.xpipe.core;

import lombok.Value;

public sealed interface FileInfo permits FileInfo.Windows, FileInfo.Unix {

    boolean explicitlyHidden();

    boolean possiblyExecutable();

    @Value
    class Windows implements FileInfo {

        String attributes;

        @Override
        public boolean explicitlyHidden() {
            return attributes.contains("h");
        }

        @Override
        public boolean possiblyExecutable() {
            return true;
        }
    }

    @Value
    class Unix implements FileInfo {

        String permissions;
        Integer uid;
        String user;
        Integer gid;
        String group;

        @Override
        public boolean explicitlyHidden() {
            return false;
        }

        @Override
        public boolean possiblyExecutable() {
            return permissions == null || permissions.contains("x");
        }
    }
}
