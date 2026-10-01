package io.xpipe.app.issue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AttachmentHelper {

    public static Path compressZipfile(Path sourceDir, Path outputFile) throws IOException {
        try (ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(outputFile.toFile()))) {
            compressDirectoryToZipfile(sourceDir, sourceDir, zipFile);
        }
        return outputFile;
    }

    private static void compressDirectoryToZipfile(Path rootDir, Path sourceDir, ZipOutputStream out)
            throws IOException {
        var files = sourceDir.toFile().listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                compressDirectoryToZipfile(rootDir, sourceDir.resolve(file.getName()), out);
            } else {
                ZipEntry entry = new ZipEntry(
                        rootDir.relativize(sourceDir).resolve(file.getName()).toString());
                out.putNextEntry(entry);

                try (FileInputStream in = new FileInputStream(sourceDir.resolve(file.getName()).toString())) {
                    in.transferTo(out);
                }
            }
        }
    }
}
