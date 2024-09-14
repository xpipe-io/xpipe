package io.xpipe.app.resources;

import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SystemIcons {

    private static final List<AutoSystemIcon> AUTO_SYSTEM_ICONS = List.of(new AutoSystemIcon("opnsense", "OpnSense",sc -> {
        return sc.getOriginalShellDialect() == ShellDialects.OPNSENSE;
    }));

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
                    if (name.contains("-dark") || name.contains("-40")) {
                        continue;
                    }
                    var base = name.replaceAll("-24", "");
                    if (AUTO_SYSTEM_ICONS.stream().anyMatch(autoSystemIcon -> autoSystemIcon.getIconName().equals(base))) {
                        continue;
                    }
                    var displayName = base.replaceAll("-", " ");
                    SYSTEM_ICONS.add(new SystemIcon(base, displayName));
                }
            }
        });
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
        for (AutoSystemIcon autoSystemIcon : AUTO_SYSTEM_ICONS) {
            if (autoSystemIcon.isApplicable(sc)) {
                return Optional.of(autoSystemIcon);
            }
        }
        return Optional.empty();
    }

    public static List<SystemIcon> getSystemIcons() {
        return SYSTEM_ICONS;
    }
}
