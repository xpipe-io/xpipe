package io.xpipe.app.browser.icon;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.store.FileSystem;

import java.util.Arrays;

public interface FolderIconFactory {

    class SimpleDirectory implements FolderIconFactory {

        private final IconVariant closed;
        private final IconVariant open;
        private final String[] names;

        public SimpleDirectory(IconVariant closed, IconVariant open, String... names) {
            this.closed = closed;
            this.open = open;
            this.names = names;
        }

        @Override
        public String getIcon(FileSystem.FileEntry entry, boolean open) {
            if (!entry.isDirectory()) {
                return null;
            }

            return Arrays.stream(names).anyMatch(name -> FileNames.getFileName(entry.getPath())
                            .equalsIgnoreCase(name))
                    ? (open ? this.open.getIcon() : this.closed.getIcon())
                    : null;
        }
    }

    String getIcon(FileSystem.FileEntry entry, boolean open);
}
