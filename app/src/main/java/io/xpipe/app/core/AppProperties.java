package io.xpipe.app.core;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.XPipeInstallation;
import lombok.Getter;
import lombok.Value;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Value
public class AppProperties {

    private static final String EXTENSION_PATHS_PROP = "io.xpipe.app.extensions";
    private static AppProperties INSTANCE;
    boolean fullVersion;
    @Getter
    String version;
    @Getter
    String build;
    UUID buildUuid;
    String sentryUrl;
    String arch;
    @Getter
    boolean image;
    boolean staging;
    boolean useVirtualThreads;
    Path dataDir;
    boolean showcase;

    public AppProperties() {
        image = ModuleHelper.isImage();
        fullVersion = Optional.ofNullable(System.getProperty("io.xpipe.app.fullVersion")).map(Boolean::parseBoolean).orElse(false);
        version = Optional.ofNullable(System.getProperty("io.xpipe.app.version")).orElse("dev");
        build = Optional.ofNullable(System.getProperty("io.xpipe.app.build")).orElse("unknown");
        buildUuid = Optional.ofNullable(System.getProperty("io.xpipe.app.buildId")).map(UUID::fromString).orElse(UUID.randomUUID());
        sentryUrl = System.getProperty("io.xpipe.app.sentryUrl");
        arch = System.getProperty("io.xpipe.app.arch");
        staging = XPipeInstallation.isStaging();
        useVirtualThreads = Optional.ofNullable(System.getProperty("io.xpipe.app.useVirtualThreads")).map(Boolean::parseBoolean).orElse(true);
        dataDir = XPipeInstallation.getDataDir();
        showcase = Optional.ofNullable(System.getProperty("io.xpipe.app.showcase")).map(Boolean::parseBoolean).orElse(false);
    }

    public static void logSystemProperties() {
        for (var e : System.getProperties().entrySet()) {
            if (Objects.equals("user.dir", e.getKey())) {
                TrackEvent.info("Detected system property " + e.getKey() + "=" + e.getValue());
            }
        }
    }

    public static void logArguments(String[] args) {
        TrackEvent.withInfo("Detected arguments").tag("list", Arrays.asList(args)).handle();
    }

    public static void logPassedProperties() {
        TrackEvent.withInfo("Loaded properties").tag("version", INSTANCE.version).tag("build", INSTANCE.build).tag("dataDir", INSTANCE.dataDir).tag(
                "fullVersion", INSTANCE.fullVersion).build();

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

    public boolean isDeveloperMode() {
        if (AppPrefs.get() == null) {
            return false;
        }

        return AppPrefs.get().developerMode().getValue();
    }

}
