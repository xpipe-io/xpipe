package io.xpipe.app.hub.entry;

import io.xpipe.app.core.AppResources;
import io.xpipe.app.util.FilePath;
import io.xpipe.app.util.OsType;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class OsLogoRegistry {

    private static final Map<String, String> ICONS = new HashMap<>();
    private static final String LINUX_DEFAULT_24 = "linux-24.png";

    public static String getUnknownImage() {
        return ICONS.get("unknown");
    }

    public static String getImage(String name, OsType.Any type) {
        if (name == null) {
            return null;
        }

        if (name.contains("Cisco")) {
            return null;
        }

        if (ICONS.isEmpty()) {
            AppResources.with(AppResources.MAIN_MODULE, "os", file -> {
                try (var list = Files.list(file)) {
                    list.filter(path -> path.toString().endsWith(".png")
                                    && !path.toString().contains("-dark")
                                    && !path.toString().endsWith(LINUX_DEFAULT_24)
                                    && !path.toString().endsWith("-40.png"))
                            .map(path -> path.getFileName().toString())
                            .forEach(path -> {
                                var base = path.replace("-dark", "").replace("-24.png", ".svg");
                                ICONS.put(
                                        FilePath.of(base)
                                                .getBaseName()
                                                .toString()
                                                .split("-")[0],
                                        "os/" + base);
                            });
                }
            });
        }

        var found = ICONS.entrySet().stream()
                .filter(e -> !e.getKey().equals("unknown"))
                .filter(e -> name.toLowerCase().contains(e.getKey())
                        || name.toLowerCase().replaceAll("\\s+", "").contains(e.getKey()))
                .findAny()
                .map(e -> e.getValue());
        if (found.isPresent()) {
            return found.get();
        }

        if (type == OsType.SOLARIS) {
            return "os/illumos.svg";
        }

        if (type == OsType.UNIX) {
            return "os/unix.svg";
        }

        return "os/linux.svg";
    }
}
