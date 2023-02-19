package io.xpipe.app.update;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppExtensionManager;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.BusyProperty;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.XPipeDistributionType;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.util.XPipeSession;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;
import org.kohsuke.github.GHRelease;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

@Getter
public class AppUpdater {

    private static AppUpdater INSTANCE;
    private final Property<AvailableRelease> lastUpdateCheckResult = new SimpleObjectProperty<>();
    private final Property<DownloadedUpdate> downloadedUpdate = new SimpleObjectProperty<>();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final PerformedUpdate performedUpdate;

    private AppUpdater() {
        performedUpdate = AppCache.get("performedUpdate", PerformedUpdate.class, () -> null);
        var hasUpdated = performedUpdate != null;
        event("Was updated is " + hasUpdated);
        if (hasUpdated) {
            AppCache.clear("performedUpdate");
            event("Found information about recent update");
        }

        downloadedUpdate.setValue(AppCache.get("downloadedUpdate", DownloadedUpdate.class, () -> null));
        if (downloadedUpdate.getValue() != null
                && !downloadedUpdate
                        .getValue()
                        .getSourceVersion()
                        .equals(AppProperties.get().getVersion())) {
            downloadedUpdate.setValue(null);
        }
        if (!XPipeDistributionType.get().supportsUpdate()) {
            downloadedUpdate.setValue(null);
        }

        downloadedUpdate.addListener((c, o, n) -> {
            AppCache.update("downloadedUpdate", n);
        });

        lastUpdateCheckResult.setValue(AppCache.get("lastUpdateCheckResult", AvailableRelease.class, () -> null));
        if (lastUpdateCheckResult.getValue() != null
                && lastUpdateCheckResult.getValue().getSourceVersion() != null
                && !lastUpdateCheckResult
                        .getValue()
                        .getSourceVersion()
                        .equals(AppProperties.get().getVersion())) {
            lastUpdateCheckResult.setValue(null);
        }
        event("Last update check result was " + lastUpdateCheckResult.getValue());
        lastUpdateCheckResult.addListener((c, o, n) -> {
            AppCache.update("lastUpdateCheckResult", n);
        });
    }

    private static void event(String msg) {
        TrackEvent.builder().category("installer").type("info").message(msg).handle();
    }

    public static void executeUpdateOnStartupIfNeeded() {
        try {
            AppProperties.init();
            DownloadedUpdate downloaded = AppCache.get("downloadedUpdate", DownloadedUpdate.class, () -> null);
            if (downloaded != null) {
                if (!downloaded.getSourceVersion().equals(AppProperties.get().getVersion())) {
                    return;
                }

                initBare();
                if (INSTANCE.shouldPerformUpdate()) {
                    INSTANCE.executeUpdateAndClose();
                }
            }
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).handle();
        }
    }

    public static AppUpdater get() {
        return INSTANCE;
    }

    public static void initBare() {
        AppProperties.init();
        XPipeSession.init(AppProperties.get().getBuildUuid());

        var layer = AppExtensionManager.initBare();
        if (layer == null) {
            return;
        }
        ProcessControlProvider.init(layer);

        INSTANCE = new AppUpdater();
    }

    public static void init() {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = new AppUpdater();

        if (XPipeDistributionType.get().supportsUpdate()
                && XPipeDistributionType.get() != XPipeDistributionType.DEVELOPMENT) {
            ThreadHelper.create("updater", true, () -> {
                        ThreadHelper.sleep(Duration.ofMinutes(10).toMillis());
                        event("Starting background updater thread");
                        while (true) {
                            if (INSTANCE.checkForUpdate(false) != null
                                    && AppPrefs.get().automaticallyUpdate().get()) {
                                event("Performing background update");
                                INSTANCE.downloadUpdate();
                            }

                            ThreadHelper.sleep(Duration.ofHours(1).toMillis());
                        }
                    })
                    .start();
        }
    }

    private static boolean isUpdate(String currentVersion) {
        if (AppPrefs.get() != null
                && AppPrefs.get().developerMode().getValue()
                && AppPrefs.get().developerDisableUpdateVersionCheck().get()) {
            event("Bypassing version check");
            return true;
        }

        if (
        //                true
        //                ||
        !AppProperties.get().getVersion().equals(currentVersion)) {
            event("Release has a different version");
            return true;
        }

        return false;
    }

    public void refreshUpdateState() {
        if (lastUpdateCheckResult.getValue() != null
                && !isUpdate(lastUpdateCheckResult.getValue().getVersion())) {
            lastUpdateCheckResult.setValue(lastUpdateCheckResult.getValue().withUpdate(false));
        }
    }

    public void downloadUpdateAsync() {
        ThreadHelper.runAsync(() -> downloadUpdate());
    }

    public synchronized void downloadUpdate() {
        if (busy.getValue()) {
            return;
        }

        if (lastUpdateCheckResult.getValue() == null) {
            return;
        }

        if (!XPipeDistributionType.get().supportsUpdate()) {
            return;
        }

        try (var ignored = new BusyProperty(busy)) {
            event("Performing update download ...");
            try {
                var downloadFile = AppDownloads.downloadInstaller(
                        lastUpdateCheckResult.getValue().getAssetType(), lastUpdateCheckResult.getValue().version);
                var changelogString = AppDownloads.downloadChangelog(lastUpdateCheckResult.getValue().version);
                var changelog = changelogString.orElse(null);
                var rel = new DownloadedUpdate(
                        AppProperties.get().getVersion(),
                        lastUpdateCheckResult.getValue().version,
                        downloadFile,
                        changelog,
                        lastUpdateCheckResult.getValue().getAssetType());
                downloadedUpdate.setValue(rel);
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).omit().handle();
            }
        }
    }

    private boolean shouldPerformUpdate() {
        if (busy.getValue()) {
            return false;
        }

        if (downloadedUpdate.getValue() == null) {
            return false;
        }

        return true;
    }

    public void executeUpdateAndClose() {
        if (!shouldPerformUpdate()) {
            return;
        }

        var downloadFile = downloadedUpdate.getValue().getFile();
        if (!Files.exists(downloadFile)) {
            return;
        }

        event("Executing update ...");
        OperationMode.executeAfterShutdown(() -> {
            try {
                AppInstaller.installFileLocal(lastUpdateCheckResult.getValue().getAssetType(), downloadFile);
            } catch (Throwable ex) {
                ex.printStackTrace();
            } finally {
                AppCache.clear("lastUpdateCheckResult");
                AppCache.clear("downloadedUpdate");

                var performedUpdate = new PerformedUpdate(
                        downloadedUpdate.getValue().getVersion(),
                        downloadedUpdate.getValue().getBody());
                AppCache.update("performedUpdate", performedUpdate);
            }
        });
    }

    public void checkForUpdateAsync(boolean forceCheck) {
        ThreadHelper.runAsync(() -> checkForUpdate(forceCheck));
    }

    public synchronized boolean isDownloadedUpdateStillLatest() {
        var available = checkForUpdate(true);
        return downloadedUpdate.getValue() != null
                && available.getVersion().equals(downloadedUpdate.getValue().getVersion());
    }

    public synchronized AvailableRelease checkForUpdate(boolean forceCheck) {
        if (busy.getValue()) {
            return lastUpdateCheckResult.getValue();
        }

        if (!forceCheck
                && lastUpdateCheckResult.getValue() != null
                && Duration.between(lastUpdateCheckResult.getValue().getCheckTime(), Instant.now())
                                .compareTo(Duration.ofHours(1))
                        <= 0) {
            return lastUpdateCheckResult.getValue();
        }

        try (var ignored = new BusyProperty(busy)) {
            var rel = AppDownloads.getLatestSuitableRelease();
            event("Determined latest suitable release "
                    + rel.map(GHRelease::getName).orElse(null));
            lastUpdateCheckResult.setValue(null);
            if (rel.isEmpty()) {
                return null;
            }

            var isUpdate = isUpdate(rel.get().getTagName());
            try {
                var assetType = AppInstaller.getSuitablePlatformAsset();
                var ghAsset = rel.orElseThrow().listAssets().toList().stream()
                        .filter(g -> assetType.isCorrectAsset(g.getName()))
                        .findAny();
                if (ghAsset.isEmpty()) {
                    return null;
                }

                event("Selected asset " + ghAsset.get().getName());
                lastUpdateCheckResult.setValue(new AvailableRelease(
                        AppProperties.get().getVersion(),
                        rel.get().getTagName(),
                        rel.get().getHtmlUrl().toString(),
                        ghAsset.get().getBrowserDownloadUrl(),
                        assetType,
                        Instant.now(),
                        isUpdate));

            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).omit().handle();
            }
        }

        return lastUpdateCheckResult.getValue();
    }

    @Value
    @Builder
    @Jacksonized
    public static class PerformedUpdate {
        String name;
        String rawDescription;
    }

    @Value
    @Builder
    @Jacksonized
    @With
    public static class AvailableRelease {
        String sourceVersion;
        String version;
        String releaseUrl;
        String downloadUrl;
        AppInstaller.InstallerAssetType assetType;
        Instant checkTime;
        boolean isUpdate;
    }

    @Value
    @Builder
    @Jacksonized
    public static class DownloadedUpdate {
        String sourceVersion;
        String version;
        Path file;
        String body;
        AppInstaller.InstallerAssetType assetType;
    }
}
