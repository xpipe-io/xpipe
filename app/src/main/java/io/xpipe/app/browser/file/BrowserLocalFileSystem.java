package io.xpipe.app.browser.file;

import io.xpipe.app.fs.FileEntry;
import io.xpipe.app.fs.FileKind;
import io.xpipe.app.fs.FileSystem;
import io.xpipe.app.store.LocalStore;
import io.xpipe.app.util.FilePath;

import java.nio.file.Files;
import java.nio.file.Path;

public class BrowserLocalFileSystem {

    private static FileSystem localFileSystem;

    public static void init() throws Exception {
        if (localFileSystem == null) {
            localFileSystem = new LocalStore().createFileSystem();
            localFileSystem.open();
        } else {
            localFileSystem.reinitIfNeeded();
        }
    }

    public static void reset() throws Exception {
        if (localFileSystem != null) {
            localFileSystem.close();
            localFileSystem = null;
        }
    }

    public static FileEntry getLocalFileEntry(Path file) throws Exception {
        init();
        return new FileEntry(
                localFileSystem.open(),
                FilePath.of(file),
                Files.getLastModifiedTime(file).toInstant(),
                "" + Files.size(file),
                null,
                Files.isDirectory(file) ? FileKind.DIRECTORY : FileKind.FILE);
    }

    public static BrowserEntry getLocalBrowserEntry(Path file) throws Exception {
        var e = getLocalFileEntry(file);
        return new BrowserEntry(e, null);
    }
}
