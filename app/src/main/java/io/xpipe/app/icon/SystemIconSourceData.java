package io.xpipe.app.icon;

import io.xpipe.app.issue.ErrorEvent;

import lombok.Value;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

            var files = Files.walk(dir).toList();
            for (var file : files) {
                if (file.getFileName().toString().endsWith(".svg")) {
                    var name = FilenameUtils.getBaseName(file.getFileName().toString());
                    var cleanedName = name.replaceFirst("-light$", "").replaceFirst("-dark$", "");
                    var hasLightVariant = Files.exists(file.getParent().resolve(cleanedName + "-light.svg"));
                    var hasDarkVariant = Files.exists(file.getParent().resolve(cleanedName + "-dark.svg"));
                    if (hasLightVariant && !hasDarkVariant && name.endsWith("-light")) {
                        var s = new SystemIconSourceFile(source, cleanedName.toLowerCase(Locale.ROOT), file, true);
                        sourceFiles.add(s);
                        continue;
                    }

                    if (hasLightVariant && hasDarkVariant && (name.endsWith("-dark") || name.endsWith("-light"))) {
                        continue;
                    }

                    var s = new SystemIconSourceFile(source, cleanedName.toLowerCase(Locale.ROOT), file, false);
                    sourceFiles.add(s);
                }
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }
}
