package io.xpipe.app.core;

import io.xpipe.app.core.check.AppUserDirectoryCheck;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.XPipeDaemonMode;

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
    String version;
    String build;
    UUID buildUuid;
    String sentryUrl;
    String arch;
    boolean image;
    boolean staging;
    boolean useVirtualThreads;
    boolean debugThreads;
    Path dataDir;
    Path defaultDataDir;
    Path dataBinDir;
    boolean showcase;
    AppVersion canonicalVersion;
    boolean locatePtb;
    boolean locatorVersionCheck;
    boolean isTest;
    boolean autoAcceptEula;
    UUID uuid;
    boolean initialLaunch;
    boolean restarted;
    UUID sessionId;

    boolean newBuildSession;

    boolean aotTrainMode;

    boolean debugPlatformThreadAccess;

    AppArguments arguments;

    XPipeDaemonMode explicitMode;

    String devLoginPassword;

    public AppProperties(String[] args) {
        var appDir = Path.of(System.getProperty("user.dir")).resolve("app");
        Path propsFile = appDir.resolve("dev.properties");
        if (Files.exists(propsFile)) {
            try {
                Properties props = new Properties();
                props.load(Files.newInputStream(propsFile));
                props.forEach((key, value) -> {
                    // Don't overwrite existing properties
                    if (System.getProperty(key.toString()) == null) {
                        System.setProperty(key.toString(), value.toString());
                    }
                });
            } catch (IOException e) {
                ErrorEventFactory.fromThrowable(e).handle();
            }
        }
        var referenceDir = Files.exists(appDir) ? appDir : Path.of(System.getProperty("user.dir"));

        image = AppProperties.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getProtocol()
                .equals("jrt");
        arguments = AppArguments.init(args);
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
        staging = Optional.ofNullable(System.getProperty("io.xpipe.app.staging"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        devLoginPassword = System.getProperty("io.xpipe.app.loginPassword");
        useVirtualThreads = Optional.ofNullable(System.getProperty("io.xpipe.app.useVirtualThreads"))
                .map(Boolean::parseBoolean)
                .orElse(true);
        debugThreads = Optional.ofNullable(System.getProperty("io.xpipe.app.debugThreads"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        debugPlatformThreadAccess = Optional.ofNullable(System.getProperty("io.xpipe.app.debugPlatformThreadAccess"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        defaultDataDir = Path.of(System.getProperty("user.home"), isStaging() ? ".xpipe-ptb" : ".xpipe");
        dataDir = Optional.ofNullable(System.getProperty("io.xpipe.app.dataDir"))
                .map(s -> {
                    var p = Path.of(s);
                    if (!p.isAbsolute()) {
                        p = referenceDir.resolve(p);
                    }
                    return p;
                })
                .orElse(defaultDataDir);
        dataBinDir = dataDir.resolve("bin");
        showcase = Optional.ofNullable(System.getProperty("io.xpipe.app.showcase"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        canonicalVersion = AppVersion.parse(version).orElse(null);
        locatePtb = Optional.ofNullable(System.getProperty("io.xpipe.app.locator.usePtbInstallation"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        locatorVersionCheck = Optional.ofNullable(
                        System.getProperty("io.xpipe.app.locator.disableInstallationVersionCheck"))
                .map(s -> !Boolean.parseBoolean(s))
                .orElse(true);
        isTest = isJUnitTest();
        autoAcceptEula = Optional.ofNullable(System.getProperty("io.xpipe.app.acceptEula"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        restarted = Optional.ofNullable(System.getProperty("io.xpipe.app.restarted"))
                .map(Boolean::parseBoolean)
                .orElse(false);

        // We require the user dir from here
        AppUserDirectoryCheck.check(dataDir);
        AppCache.setBasePath(dataDir.resolve("cache"));
        UUID id = AppCache.getNonNull("uuid", UUID.class, () -> null);
        if (id == null) {
            uuid = UUID.randomUUID();
            AppCache.update("uuid", uuid);
        } else {
            uuid = id;
        }
        initialLaunch = AppCache.getNonNull("lastBuildId", String.class, () -> null) == null;
        sessionId = UUID.randomUUID();
        var cachedBuildId = AppCache.getNonNull("lastBuildId", String.class, () -> null);
        newBuildSession = !buildUuid.toString().equals(cachedBuildId);
        AppCache.update("lastBuildId", buildUuid);
        aotTrainMode = Optional.ofNullable(System.getProperty("io.xpipe.app.aotTrainMode"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        explicitMode = XPipeDaemonMode.getIfPresent(System.getProperty("io.xpipe.app.mode"))
                .orElse(null);
    }

    public void resetInitialLaunch() {
        AppCache.clear("lastBuildId");
    }

    private static boolean isJUnitTest() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().startsWith("org.junit.")) {
                return true;
            }
        }
        return false;
    }

    public static void logSystemProperties() {
        for (var e : System.getProperties().entrySet()) {
            if (List.of("user.dir").contains(e.getKey())) {
                TrackEvent.info("Detected system property " + e.getKey() + "=" + e.getValue());
            }
        }
    }

    public void logArguments() {
        TrackEvent.withInfo("Loaded properties")
                .tag("version", version)
                .tag("build", build)
                .tag("dataDir", dataDir)
                .tag("fullVersion", fullVersion)
                .handle();

        TrackEvent.withInfo("Received arguments")
                .tag("raw", arguments.getRawArgs())
                .tag("resolved", arguments.getResolvedArgs())
                .tag("resolvedCommand", arguments.getOpenArgs())
                .handle();

        for (var e : System.getProperties().entrySet()) {
            if (e.getKey().toString().contains("io.xpipe")) {
                TrackEvent.info("Detected xpipe property " + e.getKey() + "=" + e.getValue());
            }
        }
    }

    public static void init() {
        init(new String[0]);
    }

    public static void init(String[] args) {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = new AppProperties(args);
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
