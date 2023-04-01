package io.xpipe.app.browser.icon;

import io.xpipe.core.store.FileSystem;

import java.util.Arrays;

public interface FileIconFactory {

    class SimpleFile extends IconVariant implements FileIconFactory {

        private final String[] endings;

        public SimpleFile(String lightIcon, String darkIcon, String... endings) {
            super(lightIcon, darkIcon);
            this.endings = endings;
        }

        @Override
        public String getIcon(FileSystem.FileEntry entry) {
            if (entry.isDirectory()) {
                return null;
            }

            return Arrays.stream(endings).anyMatch(ending -> entry.getPath().endsWith(ending)) ? getIcon() : null;
        }
    }

    String getIcon(FileSystem.FileEntry entry);
}
