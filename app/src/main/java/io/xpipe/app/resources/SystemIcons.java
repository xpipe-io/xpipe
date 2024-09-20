package io.xpipe.app.resources;

import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.ShellStoreState;
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

    private static final List<SystemIcon> AUTO_SYSTEM_ICONS = List.of(
            new SystemIcon("opnsense", "opnsense") {
                @Override
                public boolean isApplicable(DataStore store) {
                    return store instanceof StatefulDataStore<?> statefulDataStore &&
                            statefulDataStore.getState() instanceof ShellStoreState shellStoreState &&
                            shellStoreState.getShellDialect() == ShellDialects.OPNSENSE;
                }
            },
            new SystemIcon("pfsense", "pfsense")  {
                @Override
                public boolean isApplicable(DataStore store) {
                    return store instanceof StatefulDataStore<?> statefulDataStore &&
                            statefulDataStore.getState() instanceof ShellStoreState shellStoreState &&
                            shellStoreState.getShellDialect() == ShellDialects.PFSENSE;
                }
            },
            new ContainerAutoSystemIcon("file-browser", "file browser", name -> name.contains("filebrowser")),
            new FileAutoSystemIcon("syncthing", "syncthing", OsType.LINUX, "~/.local/state/syncthing")
    );

    private static final List<SystemIcon> SYSTEM_ICONS = new ArrayList<>();
    private static boolean loaded = false;

    public static synchronized void init() {
        if (SYSTEM_ICONS.size() > 0) {
            return;
        }

        SYSTEM_ICONS.addAll(AUTO_SYSTEM_ICONS);
        AppResources.with(AppResources.XPIPE_MODULE, "img/system", path -> {
            try (var stream = Files.list(path)) {
                var all = stream.toList();
                for (Path file : all) {
                    var name = FilenameUtils.getBaseName(file.getFileName().toString());
                    if (name.contains("-dark") || name.contains("-16") || name.contains("-24")) {
                        continue;
                    }
                    var base = name.replaceAll("-40", "");
                    if (AUTO_SYSTEM_ICONS.stream().anyMatch(autoSystemIcon -> autoSystemIcon.getIconName().equals(base))) {
                        continue;
                    }
                    var displayName = base.replaceAll("-", " ");
                    SYSTEM_ICONS.add(new SystemIcon(base, displayName));
                }
            }
        });
        SYSTEM_ICONS.sort(Comparator.<SystemIcon, String>comparing(systemIcon -> systemIcon.getIconName()));
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        AppImages.loadDirectory(AppResources.XPIPE_MODULE,"img/system", true, false);
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

    public static Optional<SystemIcon> detectForSystem(ShellControl sc) throws Exception {
        for (var autoSystemIcon : AUTO_SYSTEM_ICONS) {
            if (autoSystemIcon.isApplicable(sc)) {
                return Optional.of(autoSystemIcon);
            }
        }
        return Optional.empty();
    }

    public static Optional<SystemIcon> detectForStore(DataStore store) {
        if (store == null) {
            return Optional.empty();
        }

        for (var autoSystemIcon : AUTO_SYSTEM_ICONS) {
            if (autoSystemIcon.isApplicable(store)) {
                return Optional.of(autoSystemIcon);
            }
        }
        return Optional.empty();
    }

    public static List<SystemIcon> getSystemIcons() {
        return SYSTEM_ICONS;
    }
}
