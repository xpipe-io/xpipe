package io.xpipe.app.icon;

import io.xpipe.app.issue.ErrorEventFactory;

import lombok.Value;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Value
public class SystemIconSourceData {

    Path directory;
    List<SystemIconSourceFile> icons;

    public static SystemIconSourceData of(SystemIconSource source) {
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
            var flatFiles = files.stream()
                    .filter(path -> Files.isRegularFile(path))
                    .filter(path -> path.toString().endsWith(".svg"))
                    .map(path -> {
                        var name = FilenameUtils.getBaseName(path.getFileName().toString());
                        var cleanedName = name.replaceFirst("-light$", "").replaceFirst("-dark$", "");
                        var cleanedPath = path.getParent().resolve(cleanedName + ".svg");
                        return cleanedPath;
                    })
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            for (var file : flatFiles) {
                var name = FilenameUtils.getBaseName(file.getFileName().toString());
                var displayName = name.toLowerCase(Locale.ROOT);
                var baseFile = file.getParent().resolve(name + ".svg");
                var hasBaseVariant = Files.exists(baseFile);
                var darkModeFile = file.getParent().resolve(name + "-light.svg");
                var hasDarkModeVariant = Files.exists(darkModeFile);
                var lightModeFile = file.getParent().resolve(name + "-dark.svg");
                var hasLightModeVariant = Files.exists(lightModeFile);

                if (hasBaseVariant) {
                    sourceFiles.add(new SystemIconSourceFile(
                            source, displayName, baseFile, SystemIconSourceFile.ColorSchemeData.DEFAULT));
                }
                if (hasLightModeVariant) {
                    sourceFiles.add(new SystemIconSourceFile(
                            source, displayName, lightModeFile, SystemIconSourceFile.ColorSchemeData.LIGHT));
                }
                if (hasDarkModeVariant) {
                    sourceFiles.add(new SystemIconSourceFile(
                            source, displayName, darkModeFile, SystemIconSourceFile.ColorSchemeData.DARK));
                }
            }
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }
    }
}
