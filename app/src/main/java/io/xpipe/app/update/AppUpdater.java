package io.xpipe.app.update;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.BusyProperty;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.XPipeDistributionType;
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

import java.io.IOException;
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
    private final boolean updateSucceeded;

    private AppUpdater() {
        performedUpdate = AppCache.get("performedUpdate", PerformedUpdate.class, () -> null);
        var hasUpdated = performedUpdate != null;
        event("Was updated is " + hasUpdated);
        if (hasUpdated) {
            AppCache.clear("performedUpdate");
            updateSucceeded = AppProperties.get().getVersion().equals(performedUpdate.getNewVersion());
            AppCache.clear("lastUpdateCheckResult");
            AppCache.clear("downloadedUpdate");
            event("Found information about recent update");
        } else {
            updateSucceeded = false;
        }

        downloadedUpdate.setValue(AppCache.get("downloadedUpdate", DownloadedUpdate.class, () -> null));

        // Check if the original version this was downloaded from is still the same
        if (downloadedUpdate.getValue() != null
                && !downloadedUpdate
                        .getValue()
                        .getSourceVersion()
                        .equals(AppProperties.get().getVersion())) {
            downloadedUpdate.setValue(null);
        }

        // Check if somehow the downloaded version is equal to the current one
        if (downloadedUpdate.getValue() != null
                && downloadedUpdate
                .getValue()
                .getVersion()
                .equals(AppProperties.get().getVersion())) {
            downloadedUpdate.setValue(null);
        }

        if (!XPipeDistributionType.get().supportsUpdate()) {
            downloadedUpdate.setValue(null);
        }

        downloadedUpdate.addListener((c, o, n) -> {
            AppCache.update("downloadedUpdate", n);
        });
        lastUpdateCheckResult.addListener((c, o, n) -> {
            if (n != null && downloadedUpdate.getValue() != null && n.getVersion().equals(downloadedUpdate.getValue().getVersion())) {
                return;
            }

            downloadedUpdate.setValue(null);
        });

        if (XPipeDistributionType.get().checkForUpdateOnStartup()) {
            refreshUpdateCheckSilent();
        }
    }

    private static void event(String msg) {
        TrackEvent.builder().category("installer").type("info").message(msg).handle();
    }

    public static AppUpdater get() {
        return INSTANCE;
    }

    public static void init() {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = new AppUpdater();
        startBackgroundUpdater();
    }

    private static void startBackgroundUpdater() {
        if (XPipeDistributionType.get().supportsUpdate()
                && XPipeDistributionType.get() != XPipeDistributionType.DEVELOPMENT) {
            ThreadHelper.create("updater", true, () -> {
                        ThreadHelper.sleep(Duration.ofMinutes(10).toMillis());
                        event("Starting background updater thread");
                        while (true) {
                            var rel = INSTANCE.refreshUpdateCheckSilent();
                            if (rel != null
                                    && AppPrefs.get().automaticallyUpdate().get() && rel.isUpdate()) {
                                event("Performing background update");
                                INSTANCE.downloadUpdate();
                            }

                            ThreadHelper.sleep(Duration.ofHours(1).toMillis());
                        }
                    })
                    .start();
        }
    }

    private static boolean isUpdate(String releaseVersion) {
        if (AppPrefs.get() != null
                && AppPrefs.get().developerMode().getValue()
                && AppPrefs.get().developerDisableUpdateVersionCheck().get()) {
            event("Bypassing version check");
            return true;
        }

        if (!AppProperties.get().getVersion().equals(releaseVersion)) {
            event("Release has a different version");
            return true;
        }

        return false;
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

        if (!lastUpdateCheckResult.getValue().isUpdate()) {
            return;
        }

        try (var ignored = new BusyProperty(busy)) {
            event("Performing update download ...");
            try {
                var downloadFile = AppDownloads.downloadInstaller(
                        lastUpdateCheckResult.getValue().getAssetType(),
                        lastUpdateCheckResult.getValue().version,
                        false);
                if (downloadFile.isEmpty()) {
                    return;
                }

                var changelogString = AppDownloads.downloadChangelog(lastUpdateCheckResult.getValue().version, false);
                var changelog = changelogString.orElse(null);
                var rel = new DownloadedUpdate(
                        AppProperties.get().getVersion(),
                        lastUpdateCheckResult.getValue().version,
                        downloadFile.get(),
                        changelog,
                        lastUpdateCheckResult.getValue().getAssetType());
                downloadedUpdate.setValue(rel);
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).omit().handle();
            }
        }
    }

    public void executeUpdateAndClose() {
        if (busy.getValue()) {
            return;
        }

        if (downloadedUpdate.getValue() == null) {
            return;
        }

        var downloadFile = downloadedUpdate.getValue().getFile();
        if (!Files.exists(downloadFile)) {
            return;
        }

        event("Executing update ...");
        OperationMode.executeAfterShutdown(() -> {
            try {
                AppInstaller.installFileLocal(downloadedUpdate.getValue().getAssetType(), downloadFile);
            } catch (Throwable ex) {
                ex.printStackTrace();
            } finally {
                var performedUpdate = new PerformedUpdate(
                        downloadedUpdate.getValue().getVersion(),
                        downloadedUpdate.getValue().getBody(),
                        downloadedUpdate.getValue().getVersion());
                AppCache.update("performedUpdate", performedUpdate);
            }
        });
    }

    public void checkForUpdateAsync() {
        ThreadHelper.runAsync(() -> refreshUpdateCheckSilent());
    }

    public synchronized AvailableRelease refreshUpdateCheckSilent() {
        try {
            return refreshUpdateCheck();
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).omit().handle();
            return null;
        }
    }

    public synchronized AvailableRelease refreshUpdateCheck() throws IOException {
        if (busy.getValue()) {
            return lastUpdateCheckResult.getValue();
        }

        try (var ignored = new BusyProperty(busy)) {
            var rel = AppDownloads.getLatestSuitableRelease();
            event("Determined latest suitable release "
                    + rel.map(GHRelease::getName).orElse(null));

            if (rel.isEmpty()) {
                lastUpdateCheckResult.setValue(null);
                return null;
            }

            // Don't update value if result is the same
            if (lastUpdateCheckResult.getValue() != null && lastUpdateCheckResult.getValue().getVersion().equals(rel.get().getTagName())) {
                return lastUpdateCheckResult.getValue();
            }

            var isUpdate = isUpdate(rel.get().getTagName());
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
        }

        return lastUpdateCheckResult.getValue();
    }

    @Value
    @Builder
    @Jacksonized
    public static class PerformedUpdate {
        String name;
        String rawDescription;
        String newVersion;
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
