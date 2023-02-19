/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

final class Utils {

    private Utils() {
        // Default constructor
    }

    public static long fileSize(Path path) {
        if (path == null) {
            return 0;
        }
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }

    public static boolean isFileHidden(Path path) {
        if (path == null) {
            return false;
        }
        try {
            return Files.isHidden(path);
        } catch (IOException e) {
            return false;
        }
    }

    public static FileTime fileMTime(Path path, LinkOption... options) {
        if (path == null) {
            return null;
        }
        try {
            return Files.getLastModifiedTime(path, options);
        } catch (IOException e) {
            return null;
        }
    }

    public static String getMimeType(Path path) {
        try {
            return Files.probeContentType(path);
        } catch (IOException e) {
            return null;
        }
    }
}
