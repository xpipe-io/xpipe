package io.xpipe.app.icon;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.resources.AppImages;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.util.ValidationException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SystemIconManager {

    private static final Path DIRECTORY =
            AppProperties.get().getDataDir().resolve("cache").resolve("icons").resolve("pool");

    private static final Map<SystemIconSource, SystemIconSourceData> LOADED = new HashMap<>();
    private static final Set<SystemIcon> ICONS = new HashSet<>();

    public static List<SystemIconSource> getEffectiveSources() {
        var prefs = AppPrefs.get().getIconSources().getValue();
        var all = new ArrayList<SystemIconSource>();
        all.add(SystemIconSource.Directory.builder()
                .path(DataStorage.get().getIconsDir())
                .id("custom")
                .build());
        all.add(SystemIconSource.GitRepository.builder()
                .remote("https://github.com/selfhst/icons")
                .id("selfhst")
                .build());
        for (var pref : prefs) {
            try {
                pref.checkComplete();
            } catch (ValidationException e) {
                ErrorEvent.fromThrowable(e).omit().expected().handle();
                continue;
            }

            if (!all.contains(pref)) {
                all.add(pref);
            }
        }
        return all;
    }

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
        reloadSources();
        SystemIconCache.refreshBuilt();
        reloadImages();
    }

    public static synchronized void reloadSources() throws Exception {
        Files.createDirectories(DIRECTORY);

        LOADED.clear();
        for (var source : getEffectiveSources()) {
            LOADED.put(source, SystemIconSourceData.of(source));
        }

        ICONS.clear();
        LOADED.forEach((source, systemIconSourceData) -> {
            systemIconSourceData.getIcons().forEach(systemIconSourceFile -> {
                var icon = new SystemIcon(source, systemIconSourceFile.getName());
                ICONS.add(icon);
            });
        });
    }

    private static void reloadImages() {
        AppImages.remove(s -> s.startsWith("icons/"));
        try {
            for (var source : getEffectiveSources()) {
                AppImages.loadRasterImages(SystemIconCache.getDirectory(source), "icons/" + source.getId());
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    private static void clearInvalidImages() {
        AppImages.remove(s -> s.startsWith("icons/"));
        try {
            for (var source : getEffectiveSources()) {
                AppImages.loadRasterImages(SystemIconCache.getDirectory(source), "icons/" + source.getId());
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    public static synchronized void reload() throws Exception {
        Files.createDirectories(DIRECTORY);
        for (var source : getEffectiveSources()) {
            source.refresh();
        }
        reloadSources();
        SystemIconCache.rebuildCache(LOADED);
        SystemIconCache.refreshBuilt();
        reloadImages();
    }

    public static Path getPoolPath() {
        return AppProperties.get()
                .getDataDir()
                .resolve("cache")
                .resolve("icons")
                .resolve("pool");
    }
}
