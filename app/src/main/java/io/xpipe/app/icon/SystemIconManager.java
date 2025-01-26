package io.xpipe.app.icon;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.resources.AppImages;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SystemIconManager {

    private static final Path DIRECTORY = AppProperties.get().getDataDir().resolve("cache").resolve("icons").resolve("pool");

    private static final Map<SystemIconSource, SystemIconSourceData> LOADED = new HashMap<>();
    private static final Set<SystemIcon> ICONS = new HashSet<>();

    public static Map<SystemIconSource, SystemIconSourceData> getSources() {
        return LOADED;
    }

    public static Set<SystemIcon> getIcons() {
        return ICONS;
    }

    public static String getIconFile(SystemIcon icon) {
        return "icons/" + icon.getSource().getId() + "/" + icon.getId() + ".svg";
    }

    public static void init() throws Exception {
        loadSources();
        SystemIconCache.refreshBuilt();
        loadImages();
    }

    public static synchronized void loadSources() throws Exception {
        Files.createDirectories(DIRECTORY);

        LOADED.clear();
        for (var source : AppPrefs.get().getIconSources().getValue()) {
            LOADED.put(source,SystemIconSourceData.of(source));
        }

        ICONS.clear();
        LOADED.forEach((source, systemIconSourceData) -> {
            systemIconSourceData.getIcons().forEach(systemIconSourceFile -> {
                var icon = new SystemIcon(source, systemIconSourceFile.getName());
                ICONS.add(icon);
            });
        });
    }

    public static void loadImages() {
        try {
            for (var source : AppPrefs.get().getIconSources().getValue()) {
                AppImages.loadRasterImages(SystemIconCache.getDirectory(source), "icons/" + source.getId());
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    public static synchronized void reload() throws Exception {
        Files.createDirectories(DIRECTORY);
        for (var source : AppPrefs.get().getIconSources().getValue()) {
            source.refresh();
        }
        loadSources();
        SystemIconCache.buildCache(LOADED);
        SystemIconCache.refreshBuilt();
    }

    public static Path getPoolPath() {
        return AppProperties.get().getDataDir().resolve("cache").resolve("icons").resolve("pool");
    }
}
