package io.xpipe.app.prefs;

import io.xpipe.app.core.AppExtensionManager;
import io.xpipe.app.core.AppResources;

import org.apache.commons.io.FilenameUtils;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public record ThirdPartyDependency(String name, String version, String licenseName, String licenseText, String link) {

    private static final List<ThirdPartyDependency> ALL = new ArrayList<>();

    public static void init() {
        for (var module : AppExtensionManager.getInstance().getContentModules()) {
            AppResources.with(module.getName(), "third-party", path -> {
                if (!Files.exists(path)) {
                    return;
                }

                try (var list = Files.list(path)) {
                    for (var p : list.filter(p -> p.getFileName().toString().endsWith(".properties"))
                            .sorted(Comparator.comparing(path1 -> path1.toString()))
                            .toList()) {
                        var props = new Properties();
                        try (var in = Files.newInputStream(p)) {
                            props.load(in);
                        }

                        var textFile = p.resolveSibling(
                                FilenameUtils.getBaseName(p.getFileName().toString()) + ".license");
                        var text = Files.readString(textFile);
                        ALL.add(new ThirdPartyDependency(
                                props.getProperty("name"),
                                props.getProperty("version"),
                                props.getProperty("license"),
                                text,
                                props.getProperty("link")));
                    }
                }
            });
        }
    }

    public static List<ThirdPartyDependency> getAll() {
        if (ALL.size() == 0) {
            init();
        }
        return ALL;
    }
}
