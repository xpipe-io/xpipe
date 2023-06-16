package io.xpipe.app.browser.icon;

import io.xpipe.app.core.AppImages;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.fxcomps.impl.SvgCache;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileSystem;
import javafx.scene.image.Image;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FileIconManager {

    @Getter
    private static final SvgCache svgCache = createCache();

    private static boolean loaded;

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
            AppImages.loadDirectory(AppResources.XPIPE_MODULE, "browser_icons");
            loaded = true;
        }
    }

    public static String getFileIcon(FileSystem.FileEntry entry, boolean open) {
        if (entry == null) {
            return null;
        }

        loadIfNecessary();

        if (entry.getKind() != FileKind.DIRECTORY) {
            for (var f : FileType.ALL) {
                if (f.matches(entry)) {
                    return getIconPath(f.getIcon());
                }
            }
        } else {
            for (var f : DirectoryType.ALL) {
                if (f.matches(entry)) {
                    return getIconPath(f.getIcon(entry, open));
                }
            }
        }

        return entry.getKind() == FileKind.DIRECTORY
                ? (open ? "default_folder_opened.svg" : "default_folder.svg")
                : "default_file.svg";
    }

    private static String getIconPath(String name) {
        return name;
    }
}
