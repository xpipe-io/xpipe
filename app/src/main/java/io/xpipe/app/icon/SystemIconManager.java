package io.xpipe.app.icon;

import io.xpipe.app.core.*;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SystemIconManager {

    private static final Path DIRECTORY =
            AppProperties.get().getDataDir().resolve("cache").resolve("icons").resolve("pool");

    private static final Set<SystemIcon> loadedIconImages = new HashSet<>();
    private static final Map<SystemIconSource, SystemIconSourceData> LOADED_SOURCES = new HashMap<>();
    private static final Set<SystemIcon> ICONS = new HashSet<>();
    private static int cacheSourceHash;
    private static int sourceHash;

    public static boolean isCacheOutdated() {
        return cacheSourceHash == 0 || sourceHash != cacheSourceHash;
    }

    public static List<SystemIconSource> getAllSources() {
        var prefs = AppPrefs.get().getIconSources().getValue();
        var all = new ArrayList<SystemIconSource>();
        all.add(SystemIconSource.Directory.builder()
                .path(DataStorage.getStorageDirectory().resolve("icons"))
                .id("custom")
                .build());
        // For chinese users, GitHub link might be unreliable
        // So use an alternative chinese mirror they can use
        all.add(SystemIconSource.GitRepository.builder()
                .remote("https://github.com/selfhst/icons")
                .id("selfhst")
                .build());
        for (var pref : prefs) {
            try {
                pref.checkComplete();
            } catch (ValidationException e) {
                // This can be expected for synced directory sources
                continue;
            }

            if (!all.contains(pref)) {
                all.add(pref);
            }
        }
        return all;
    }

    public static List<SystemIconSource> getEffectiveSources() {
        var all = getAllSources();
        var disabled = AppCache.getNonNull("disabledIconSources", Set.class, () -> Set.<String>of());
        all.removeIf(systemIconSource -> disabled.contains(systemIconSource.getId()));
        return all;
    }

    public static Set<SystemIcon> getIcons() {
        return ICONS;
    }

    public static synchronized String getAndLoadIconFile(SystemIcon icon) {
        var id = "icons/" + icon.getSource().getId() + "/" + icon.getId() + ".svg";
        if (loadedIconImages.contains(icon)) {
            return id;
        }

        var dir = SystemIconCache.getDirectory(icon.getSource());
        var res = AppDisplayScale.hasOnlyDefaultDisplayScale() ? List.of(16, 24, 40) : List.of(16, 24, 40, 80);
        var files = new ArrayList<Path>();
        for (Integer re : res) {
            files.add(dir.resolve(icon.getId() + "-" + re + ".png"));
            files.add(dir.resolve(icon.getId() + "-" + re + "-dark.png"));
        }
        for (var file : files) {
            if (Files.isRegularFile(file)) {
                AppImages.loadImage(file, "icons/" + icon.getSource().getId() + "/" + file.getFileName());
            }
        }

        loadedIconImages.add(icon);
        return id;
    }

    public static Optional<SystemIcon> getIcon(String id) {
        var split = id.split("/");
        if (split.length == 2) {
            var source = split[0];
            var foundSource = getAllSources().stream()
                    .filter(systemIconSource -> systemIconSource.getId().equals(source))
                    .findFirst();
            if (foundSource.isEmpty()) {
                return Optional.empty();
            }

            var icon = new SystemIcon(foundSource.get(), split[1]);
            var foundIcon = ICONS.contains(icon);
            return foundIcon ? Optional.of(icon) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    private static synchronized int calculateSourceHash() {
        var total = 0;
        var set = false;
        for (var e : LOADED_SOURCES.entrySet()) {
            total += e.getKey().getPath().hashCode();
            for (SystemIconSourceFile icon : e.getValue().getIcons()) {
                total += icon.getFile().toString().hashCode();
                set = true;
            }
        }

        if (set) {
            total += AppPrefs.get().preferMonochromeIcons().get() ? 1 : 0;
            total += SystemIconCache.VERSION;
        }

        return total != 0 ? total : set ? -1 : 0;
    }

    public static void init() throws Exception {
        cacheSourceHash = SystemIconCache.getCacheSourceHash();
        reloadSources();
        sourceHash = calculateSourceHash();
        AppPrefs.get().preferMonochromeIcons().addListener((observableValue, o, n) -> {
            sourceHash = calculateSourceHash();
        });
        AppPrefs.get().getIconSources().addListener((observableValue, o, n) -> {
            sourceHash = calculateSourceHash();
        });
    }

    public static void initAdditional() {
        for (var source : getEffectiveSources()) {
            if (!LOADED_SOURCES.containsKey(source)) {
                var data = SystemIconSourceData.of(source);
                LOADED_SOURCES.put(source, data);
                data.getIcons().forEach(systemIconSourceFile -> {
                    var icon = new SystemIcon(source, systemIconSourceFile.getName());
                    ICONS.add(icon);
                });
            }
        }
        sourceHash = calculateSourceHash();
    }

    public static synchronized void reloadSources() throws Exception {
        Files.createDirectories(DIRECTORY);

        LOADED_SOURCES.clear();
        for (var source : getEffectiveSources()) {
            LOADED_SOURCES.put(source, SystemIconSourceData.of(source));
        }

        ICONS.clear();
        LOADED_SOURCES.forEach((source, systemIconSourceData) -> {
            systemIconSourceData.getIcons().forEach(systemIconSourceFile -> {
                var icon = new SystemIcon(source, systemIconSourceFile.getName());
                ICONS.add(icon);
            });
        });
    }

    public static void prepareUsedIconImages() {
        for (DataStoreEntry storeEntry : DataStorage.get().getStoreEntries()) {
            storeEntry.getEffectiveIconFile();
        }
    }

    private static synchronized void reloadImages() {
        AppImages.remove(s -> s.startsWith("icons/"));
        loadedIconImages.clear();

        try {
            for (var loadedIconImage : ICONS) {
                getAndLoadIconFile(loadedIconImage);
            }
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }
    }

    public static synchronized void rebuild() throws Exception {
        Files.createDirectories(DIRECTORY);
        for (var source : getEffectiveSources()) {
            try {
                source.refresh();
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).expected().handle();
            }
        }
        reloadSources();
        sourceHash = calculateSourceHash();
        SystemIconCache.rebuildCache(LOADED_SOURCES, sourceHash);
        cacheSourceHash = sourceHash;
        reloadImages();
    }

    public static synchronized void reloadSourceHashes() throws Exception {
        Files.createDirectories(DIRECTORY);
        reloadSources();
        sourceHash = calculateSourceHash();
    }

    public static synchronized void loadAllAvailableIconImages() {
        for (SystemIcon icon : getIcons()) {
            getAndLoadIconFile(icon);
        }
    }

    public static Path getPoolPath() {
        return AppProperties.get()
                .getDataDir()
                .resolve("cache")
                .resolve("icons")
                .resolve("pool");
    }
}
