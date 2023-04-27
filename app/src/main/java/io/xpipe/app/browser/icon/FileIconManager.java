package io.xpipe.app.browser.icon;

import io.xpipe.app.core.AppImages;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.fxcomps.impl.SvgCache;
import io.xpipe.core.store.FileSystem;
import javafx.scene.image.Image;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class FileIconManager {

    private static final List<FileIconFactory> factories = new ArrayList<>();
    private static final List<FolderIconFactory> folderFactories = new ArrayList<>();
    @Getter
    private static SvgCache svgCache = createCache();
    private static boolean loaded;

    private static void loadDefinitions() {
        AppResources.with(AppResources.XPIPE_MODULE, "browser_icons/file_list.txt", path -> {
            try (var reader =
                    new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    var split = line.split("\\|");
                    var id = split[0].trim();
                    var filter = Arrays.stream(split[1].split(","))
                            .map(s -> {
                                var r = s.trim();
                                if (r.startsWith(".")) {
                                    return r;
                                }

                                if (r.contains(".")) {
                                    return r;
                                }

                                return "." + r;
                            })
                            .toList();
                    var darkIcon = split[2].trim();
                    var lightIcon = split.length > 3 ? split[3].trim() : darkIcon;
                    factories.add(new FileIconFactory.SimpleFile(lightIcon, darkIcon, filter.toArray(String[]::new)));
                }
            }
        });

        folderFactories.addAll(List.of(new FolderIconFactory.SimpleDirectory(
                new IconVariant("default_root_folder.svg"), new IconVariant("default_root_folder_opened.svg"), "")));

        AppResources.with(AppResources.XPIPE_MODULE, "browser_icons/folder_list.txt", path -> {
            try (var reader =
                    new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    var split = line.split("\\|");
                    var id = split[0].trim();
                    var filter = Arrays.stream(split[1].split(","))
                            .map(s -> {
                                var r = s.trim();
                                if (r.startsWith(".")) {
                                    return r;
                                }

                                if (r.contains(".")) {
                                    return r;
                                }

                                return "." + r;
                            })
                            .toList();

                    var closedIcon = split[2].trim();
                    var openIcon = split[3].trim();

                    var lightClosedIcon = split.length > 4 ? split[4].trim() : closedIcon;
                    var lightOpenIcon = split.length > 4 ? split[5].trim() : openIcon;

                    folderFactories.add(new FolderIconFactory.SimpleDirectory(
                            new IconVariant(lightClosedIcon, closedIcon),
                            new IconVariant(lightOpenIcon, openIcon),
                            filter.toArray(String[]::new)));
                }
            }
        });
    }

    private static SvgCache createCache() {
        return new SvgCache() {

            private final Map<String, Image> images = new HashMap<>();

            @Override
            public synchronized void put(String image, Image value) {
                images.put(image, value);
            }

            @Override
            public synchronized Optional<Image> getCached(String image) {
                return Optional.ofNullable(images.get(image));
            }
        };
    }

    public static synchronized void loadIfNecessary() {
        if (!loaded) {
            loadDefinitions();
            AppImages.loadDirectory(AppResources.XPIPE_MODULE, "browser_icons");
            loaded = true;
        }
    }

    public static String getFileIcon(FileSystem.FileEntry entry, boolean open) {
        if (entry == null) {
            return null;
        }

        loadIfNecessary();

        if (!entry.isDirectory()) {
            for (var f : factories) {
                var icon = f.getIcon(entry);
                if (icon != null) {
                    return getIconPath(icon);
                }
            }
        } else {
            for (var f : folderFactories) {
                var icon = f.getIcon(entry, open);
                if (icon != null) {
                    return getIconPath(icon);
                }
            }
        }

        return entry.isDirectory() ? (open ? "default_folder_opened.svg" : "default_folder.svg") : "default_file.svg";
    }

    private static String getIconPath(String name) {
        return name;
    }
}
