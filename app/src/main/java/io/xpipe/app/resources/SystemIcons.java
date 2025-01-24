package io.xpipe.app.resources;

import io.xpipe.core.process.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StatefulDataStore;

import org.apache.commons.io.FilenameUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SystemIcons {

    private static final List<SystemIcon> SYSTEM_ICONS = new ArrayList<>();
    private static boolean loaded = false;

    public static synchronized void init() {
        if (SYSTEM_ICONS.size() > 0) {
            return;
        }
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        AppImages.loadDirectory(AppResources.XPIPE_MODULE, "img/system", true, false);
        loaded = true;
    }

    public static Optional<SystemIcon> getForId(String id) {
        if (id == null) {
            return Optional.empty();
        }

        for (SystemIcon systemIcon : SYSTEM_ICONS) {
            if (systemIcon.getIconName().equals(id)) {
                return Optional.of(systemIcon);
            }
        }
        return Optional.empty();
    }

    public static List<SystemIcon> getSystemIcons() {
        return SYSTEM_ICONS;
    }
}
