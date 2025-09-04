package io.xpipe.app.browser.file;

import io.xpipe.app.browser.icon.BrowserIconDirectoryType;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.core.FileKind;

import lombok.Getter;

@Getter
public class BrowserEntry {

    private final BrowserFileListModel model;
    private final FileEntry rawFileEntry;
    private final BrowserIconFileType fileType;
    private final BrowserIconDirectoryType directoryType;

    public BrowserEntry(FileEntry rawFileEntry, BrowserFileListModel model) {
        this.rawFileEntry = rawFileEntry;
        this.model = model;
        this.fileType = fileType(rawFileEntry);
        this.directoryType = directoryType(rawFileEntry);
    }

    private static BrowserIconFileType fileType(FileEntry rawFileEntry) {
        if (rawFileEntry == null) {
            return null;
        }
        rawFileEntry = rawFileEntry.resolved();

        if (rawFileEntry.getKind() != FileKind.FILE) {
            return null;
        }

        for (var f : BrowserIconFileType.getAll()) {
            if (f.matches(rawFileEntry)) {
                return f;
            }
        }

        return null;
    }

    private static BrowserIconDirectoryType directoryType(FileEntry rawFileEntry) {
        if (rawFileEntry == null) {
            return null;
        }
        rawFileEntry = rawFileEntry.resolved();

        if (rawFileEntry.getKind() != FileKind.DIRECTORY) {
            return null;
        }

        for (var f : BrowserIconDirectoryType.getAll()) {
            if (f.matches(rawFileEntry)) {
                return f;
            }
        }

        return null;
    }

    public String getIcon() {
        if (fileType != null) {
            return fileType.getIcon();
        } else if (directoryType != null) {
            return directoryType.getIcon();
        } else {
            return rawFileEntry != null && rawFileEntry.resolved().getKind() == FileKind.DIRECTORY
                    ? "browser/default_folder.svg"
                    : "browser/default_file.svg";
        }
    }

    public String getFileName() {
        return getRawFileEntry().getPath().getFileName();
    }
}
