package io.xpipe.app.icon;

import io.xpipe.app.issue.ErrorEvent;
import lombok.Value;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Value
public class SystemIconSourceData {

    Path directory;
    List<SystemIconSourceFile> icons;

    public static SystemIconSourceData of(SystemIconSource source) throws IOException {
        var target = source.getPath();
        var list = new ArrayList<SystemIconSourceFile>();
        walkTree(source, target, list);
        return new SystemIconSourceData(target, list);
    }

    private static void walkTree(SystemIconSource source, Path dir, List<SystemIconSourceFile> sourceFiles) {
        try {
            if (!Files.isDirectory(dir)) {
                return;
            }

            var files = Files.walk(dir, FileVisitOption.FOLLOW_LINKS).toList();
            for (var file : files) {
                if (file.getFileName().toString().endsWith(".svg")) {
                    var name = FilenameUtils.getBaseName(file.getFileName().toString());
                    var cleanedName = name.replaceFirst("-light$", "").replaceFirst("-dark$", "");
                    if (name.endsWith("-light") || name.endsWith("-dark")) {
                        var s = new SystemIconSourceFile(source, cleanedName, file, name.endsWith("-dark"));
                        sourceFiles.add(s);
                        continue;
                    }

                    var bothVariants = Files.exists(file.getParent().resolve(cleanedName + "-light.svg")) && Files.exists(
                            file.getParent().resolve(cleanedName + "-dark.svg"));
                    if (!bothVariants) {
                        var s = new SystemIconSourceFile(source, cleanedName, file, name.endsWith("-dark"));
                        sourceFiles.add(s);
                    }
                }
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }
}
