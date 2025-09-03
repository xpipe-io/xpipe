package io.xpipe.app.core;

import io.xpipe.app.core.check.AppUserDirectoryCheck;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.XPipeDaemonMode;

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
    boolean developerMode;
    boolean newBuildSession;
    boolean aotTrainMode;
    boolean debugPlatformThreadAccess;
    boolean persistData;
    AppArguments arguments;
    XPipeDaemonMode explicitMode;
    String devLoginPassword;
    boolean logToSysOut;
    boolean logToFile;
    boolean logPlatformDebug;
    String logLevel;

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
        fullVersion = Optional.ofNullable(System.getProperty(AppNames.propertyName("fullVersion")))
                .map(Boolean::parseBoolean)
                .orElse(false);
        version = Optional.ofNullable(System.getProperty(AppNames.propertyName("version")))
                .orElse("dev");
        build = Optional.ofNullable(System.getProperty(AppNames.propertyName("build")))
                .orElse("unknown");
        buildUuid = Optional.ofNullable(System.getProperty(AppNames.propertyName("buildId")))
                .map(UUID::fromString)
                .orElse(UUID.randomUUID());
        sentryUrl = System.getProperty(AppNames.propertyName("sentryUrl"));
        arch = System.getProperty(AppNames.propertyName("arch"));
        staging = Optional.ofNullable(System.getProperty(AppNames.propertyName("staging")))
                .map(Boolean::parseBoolean)
                .orElse(false);
        devLoginPassword = System.getProperty(AppNames.propertyName("loginPassword"));
        useVirtualThreads = Optional.ofNullable(System.getProperty(AppNames.propertyName("useVirtualThreads")))
                .map(Boolean::parseBoolean)
                .orElse(true);
        debugThreads = Optional.ofNullable(System.getProperty(AppNames.propertyName("debugThreads")))
                .map(Boolean::parseBoolean)
                .orElse(false);
        debugPlatformThreadAccess = Optional.ofNullable(
                        System.getProperty(AppNames.propertyName("debugPlatformThreadAccess")))
                .map(Boolean::parseBoolean)
                .orElse(false);
        defaultDataDir = AppSystemInfo.ofCurrent().getUserHome().resolve(isStaging() ? ".xpipe-ptb" : ".xpipe");
        dataDir = Optional.ofNullable(System.getProperty(AppNames.propertyName("dataDir")))
                .map(s -> {
                    var p = Path.of(s);
                    if (!p.isAbsolute()) {
                        p = referenceDir.resolve(p);
                    }
                    return p;
                })
                .orElse(defaultDataDir);
        showcase = Optional.ofNullable(System.getProperty(AppNames.propertyName("showcase")))
                .map(Boolean::parseBoolean)
                .orElse(false);
        canonicalVersion = AppVersion.parse(version).orElse(null);
        locatePtb = Optional.ofNullable(System.getProperty(AppNames.propertyName("locatorUsePtbInstallation")))
                .map(Boolean::parseBoolean)
                .orElse(false);
        locatorVersionCheck = Optional.ofNullable(
                        System.getProperty(AppNames.propertyName("locatorDisableInstallationVersionCheck")))
                .map(s -> !Boolean.parseBoolean(s))
                .orElse(true);
        isTest = isJUnitTest();
        autoAcceptEula = Optional.ofNullable(System.getProperty(AppNames.propertyName("acceptEula")))
                .map(Boolean::parseBoolean)
                .orElse(false);
        restarted = Optional.ofNullable(System.getProperty(AppNames.propertyName("restarted")))
                .map(Boolean::parseBoolean)
                .orElse(false);
        developerMode = Optional.ofNullable(System.getProperty(AppNames.propertyName("developerMode")))
                .map(Boolean::parseBoolean)
                .orElse(false);
        persistData = Optional.ofNullable(System.getProperty(AppNames.propertyName("persistData")))
                .map(Boolean::parseBoolean)
                .orElse(true);
        logToSysOut = Optional.ofNullable(System.getProperty(AppNames.propertyName("writeSysOut")))
                .map(Boolean::parseBoolean)
                .orElse(false);
        logToFile = Optional.ofNullable(System.getProperty(AppNames.propertyName("writeLogs")))
                .map(Boolean::parseBoolean)
                .orElse(true);
        logPlatformDebug = Optional.ofNullable(System.getProperty(AppNames.propertyName("debugPlatform")))
                .map(Boolean::parseBoolean)
                .orElse(false);
        logLevel = Optional.ofNullable(System.getProperty(AppNames.propertyName("logLevel")))
                .filter(s -> AppLogs.LOG_LEVELS.contains(s))
                .orElse("info");

        // We require the user dir from here
        AppUserDirectoryCheck.check(dataDir);
        AppCache.setBasePath(dataDir.resolve("cache"));
        dataBinDir = dataDir.resolve("cache", "bin");
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
        aotTrainMode = Optional.ofNullable(System.getProperty(AppNames.propertyName("aotTrainMode")))
                .map(Boolean::parseBoolean)
                .orElse(false);
        explicitMode = XPipeDaemonMode.getIfPresent(System.getProperty(AppNames.propertyName("mode")))
                .orElse(null);
    }

    private static boolean isJUnitTest() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().startsWith("org.junit.")) {
                return true;
            }
        }
        return false;
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

    public void resetInitialLaunch() {
        AppCache.clear("lastBuildId");
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
            if (e.getKey().toString().contains(AppNames.ofCurrent().getGroupName())) {
                TrackEvent.debug("Detected app property " + e.getKey() + "=" + e.getValue());
            }
        }
    }

    public boolean isDevelopmentEnvironment() {
        return !isImage() && isDeveloperMode();
    }

    public Optional<AppVersion> getCanonicalVersion() {
        return Optional.ofNullable(canonicalVersion);
    }
}
