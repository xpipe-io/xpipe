package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.util.ModuleHelper;
import lombok.Getter;
import lombok.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Value
public class AppProperties {

    private static AppProperties INSTANCE;
    boolean fullVersion;

    @Getter
    String version;

    @Getter
    String build;

    UUID buildUuid;
    String sentryUrl;
    String arch;
    List<String> languages;

    @Getter
    boolean image;

    boolean staging;
    boolean useVirtualThreads;
    boolean debugThreads;
    Path dataDir;
    boolean showcase;
    AppVersion canonicalVersion;
    boolean locatePtb;
    boolean locatorVersionCheck;

    public AppProperties() {
        var appDir = Path.of(System.getProperty("user.dir")).resolve("app");
        Path propsFile = appDir.resolve("dev.properties");
        if (Files.exists(propsFile)) {
            try {
                Properties props = new Properties();
                props.load(Files.newInputStream(propsFile));
                props.forEach((key, value) -> System.setProperty(key.toString(), value.toString()));
            } catch (IOException e) {
                ErrorEvent.fromThrowable(e).handle();
            }
        }

        image = ModuleHelper.isImage();
        fullVersion = Optional.ofNullable(System.getProperty("io.xpipe.app.fullVersion"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        version =
                Optional.ofNullable(System.getProperty("io.xpipe.app.version")).orElse("dev");
        build = Optional.ofNullable(System.getProperty("io.xpipe.app.build")).orElse("unknown");
        buildUuid = Optional.ofNullable(System.getProperty("io.xpipe.app.buildId"))
                .map(UUID::fromString)
                .orElse(UUID.randomUUID());
        sentryUrl = System.getProperty("io.xpipe.app.sentryUrl");
        arch = System.getProperty("io.xpipe.app.arch");
        languages = Arrays.stream(System.getProperty("io.xpipe.app.languages").split(","))
                .sorted()
                .toList();
        staging = Optional.ofNullable(System.getProperty("io.xpipe.app.staging"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        useVirtualThreads = Optional.ofNullable(System.getProperty("io.xpipe.app.useVirtualThreads"))
                .map(Boolean::parseBoolean)
                .orElse(true);
        debugThreads = Optional.ofNullable(System.getProperty("io.xpipe.app.debugThreads"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        dataDir = Optional.ofNullable(System.getProperty("io.xpipe.app.dataDir"))
                .map(s -> {
                    var p = Path.of(s);
                    if (!p.isAbsolute()) {
                        p = appDir.resolve(p);
                    }
                    return p;
                })
                .orElse(Path.of(System.getProperty("user.home"), isStaging() ? ".xpipe-ptb" : ".xpipe"));
        showcase = Optional.ofNullable(System.getProperty("io.xpipe.app.showcase"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        canonicalVersion = AppVersion.parse(version).orElse(null);
        locatePtb = Optional.ofNullable(System.getProperty("io.xpipe.app.locator.usePtbInstallation"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        locatorVersionCheck = Optional.ofNullable(System.getProperty("io.xpipe.app.locator.disableInstallationVersionCheck"))
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    public static void logSystemProperties() {
        for (var e : System.getProperties().entrySet()) {
            if (List.of("user.dir").contains(e.getKey())) {
                TrackEvent.info("Detected system property " + e.getKey() + "=" + e.getValue());
            }
        }
    }

    public static void logArguments(String[] args) {
        TrackEvent.withInfo("Detected arguments")
                .tag("list", Arrays.asList(args))
                .handle();
    }

    public static void logPassedProperties() {
        TrackEvent.withInfo("Loaded properties")
                .tag("version", INSTANCE.version)
                .tag("build", INSTANCE.build)
                .tag("dataDir", INSTANCE.dataDir)
                .tag("fullVersion", INSTANCE.fullVersion)
                .build();

        for (var e : System.getProperties().entrySet()) {
            if (e.getKey().toString().contains("io.xpipe")) {
                TrackEvent.info("Detected xpipe property " + e.getKey() + "=" + e.getValue());
            }
        }
    }

    public static void init() {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = new AppProperties();
    }

    public static AppProperties get() {
        return INSTANCE;
    }

    public boolean isDevelopmentEnvironment() {
        return !AppProperties.get().isImage() && AppProperties.get().isDeveloperMode();
    }

    public boolean isDeveloperMode() {
        if (AppPrefs.get() == null) {
            return false;
        }

        return AppPrefs.get().developerMode().getValue();
    }

    public Optional<AppVersion> getCanonicalVersion() {
        return Optional.ofNullable(canonicalVersion);
    }
}
