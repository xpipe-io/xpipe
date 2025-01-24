package io.xpipe.app.icon;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.resources.AppImages;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SystemIconManager {

    private static final Path DIRECTORY = AppProperties.get().getDataDir().resolve("cache").resolve("icons").resolve("pool");

    private static final List<SystemIconSource> SOURCES = java.util.List.of(
            new SystemIconSource.GitRepository("https://github.com/selfhst/icons", "selfhst"));

    private static final Map<SystemIconSource, SystemIconSourceData> LOADED = new HashMap<>();
    private static final Set<SystemIcon> ICONS = new HashSet<>();

    public static Set<SystemIcon> getIcons() {
        return ICONS;
    }

    public static String getIconFile(SystemIcon icon) {
        return "icons/" + icon.getSource().getId() + "/" + icon.getId() + ".svg";
    }

    public static void loadSources() throws Exception {
        Files.createDirectories(DIRECTORY);

        LOADED.clear();
        for (var source : SOURCES) {
            LOADED.put(source,SystemIconSourceData.of(source));
        }

        ICONS.clear();
        SystemIconCache.buildCache(LOADED);
        LOADED.forEach((source, systemIconSourceData) -> {
            systemIconSourceData.getIcons().forEach(systemIconSourceFile -> {
                var icon = new SystemIcon(source, systemIconSourceFile.getName());
                ICONS.add(icon);
            });
        });
    }

    public static void loadImages() {
        try {
            for (var source : SOURCES) {
                AppImages.loadRasterImages(SystemIconCache.getDirectory(source), "icons/" + source.getId());
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    public static void reload() throws Exception {
        Files.createDirectories(DIRECTORY);
        for (var source : SOURCES) {
            source.refresh();
        }
        loadSources();
    }

    public static Path getPoolPath() {
        return AppProperties.get().getDataDir().resolve("cache").resolve("icons").resolve("pool");
    }
}
