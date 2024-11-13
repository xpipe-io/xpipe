package io.xpipe.app.update;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

@SuppressWarnings("InfiniteLoopStatement")
@Getter
public abstract class UpdateHandler {

    protected final Property<AvailableRelease> lastUpdateCheckResult = new SimpleObjectProperty<>();
    protected final Property<PreparedUpdate> preparedUpdate = new SimpleObjectProperty<>();
    protected final BooleanProperty busy = new SimpleBooleanProperty();
    protected final PerformedUpdate performedUpdate;
    protected final boolean updateSucceeded;

    protected UpdateHandler(boolean startBackgroundThread) {
        performedUpdate = AppCache.getNonNull("performedUpdate", PerformedUpdate.class, () -> null);
        var hasUpdated = performedUpdate != null;
        event("Was updated is " + hasUpdated);
        if (hasUpdated) {
            AppCache.clear("performedUpdate");
            updateSucceeded = AppProperties.get().getVersion().equals(performedUpdate.getNewVersion());
            AppCache.clear("preparedUpdate");
            event("Found information about recent update");
        } else {
            updateSucceeded = false;
        }

        preparedUpdate.setValue(AppCache.getNonNull("preparedUpdate", PreparedUpdate.class, () -> null));

        // Check if the original version this was downloaded from is still the same
        if (preparedUpdate.getValue() != null
                && (!preparedUpdate
                                .getValue()
                                .getSourceVersion()
                                .equals(AppProperties.get().getVersion())
                        || !XPipeDistributionType.get()
                                .getId()
                                .equals(preparedUpdate.getValue().getSourceDist()))) {
            preparedUpdate.setValue(null);
        }

        // Check if somehow the downloaded version is equal to the current one
        if (preparedUpdate.getValue() != null
                && preparedUpdate
                        .getValue()
                        .getVersion()
                        .equals(AppProperties.get().getVersion())) {
            preparedUpdate.setValue(null);
        }

        // Check if file has been deleted
        if (preparedUpdate.getValue() != null
                && preparedUpdate.getValue().getFile() != null
                && !Files.exists(preparedUpdate.getValue().getFile())) {
            preparedUpdate.setValue(null);
        }

        preparedUpdate.addListener((c, o, n) -> {
            AppCache.update("preparedUpdate", n);
        });
        lastUpdateCheckResult.addListener((c, o, n) -> {
            if (n != null
                    && preparedUpdate.getValue() != null
                    && n.isUpdate()
                    && n.getVersion().equals(preparedUpdate.getValue().getVersion())) {
                return;
            }

            preparedUpdate.setValue(null);
        });

        if (startBackgroundThread) {
            startBackgroundUpdater();
        }
    }

    private void startBackgroundUpdater() {
        ThreadHelper.createPlatformThread("updater", true, () -> {
                    var checked = false;
                    ThreadHelper.sleep(Duration.ofMinutes(5).toMillis());
                    event("Starting background updater thread");
                    while (true) {
                        if (AppPrefs.get().automaticallyUpdate().get()
                                || AppPrefs.get().checkForSecurityUpdates().get()) {
                            event("Performing background update");
                            refreshUpdateCheckSilent(
                                    !checked,
                                    !AppPrefs.get().automaticallyUpdate().get());
                            checked = true;
                            prepareUpdate();
                        }

                        ThreadHelper.sleep(Duration.ofHours(1).toMillis());
                    }
                })
                .start();
    }

    protected void event(String msg) {
        TrackEvent.builder().type("info").message(msg).handle();
    }

    protected final boolean isUpdate(String releaseVersion) {
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

    public final AvailableRelease refreshUpdateCheckSilent(boolean first, boolean securityOnly) {
        try {
            return refreshUpdateCheck(first, securityOnly);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).discard().handle();
            return null;
        }
    }

    public final void prepareUpdate() {
        if (busy.getValue()) {
            return;
        }

        if (lastUpdateCheckResult.getValue() == null) {
            return;
        }

        if (!lastUpdateCheckResult.getValue().isUpdate()) {
            return;
        }

        if (preparedUpdate.getValue() != null) {
            if (lastUpdateCheckResult
                    .getValue()
                    .getVersion()
                    .equals(preparedUpdate.getValue().getVersion())) {
                event("Update is already prepared ...");
                return;
            }
        }

        try (var ignored = new BooleanScope(busy).start()) {
            event("Performing update download ...");
            prepareUpdateImpl();
        }
    }

    public abstract Region createInterface();

    public void prepareUpdateImpl() {
        var changelogString =
                AppDownloads.downloadChangelog(lastUpdateCheckResult.getValue().getVersion(), false);
        var changelog = changelogString.orElse(null);

        var rel = new PreparedUpdate(
                AppProperties.get().getVersion(),
                XPipeDistributionType.get().getId(),
                lastUpdateCheckResult.getValue().getVersion(),
                lastUpdateCheckResult.getValue().getReleaseUrl(),
                null,
                changelog,
                lastUpdateCheckResult.getValue().getAssetType(),
                lastUpdateCheckResult.getValue().isSecurityOnly());
        preparedUpdate.setValue(rel);
    }

    public final void executeUpdateAndClose() {
        if (busy.getValue()) {
            return;
        }

        if (preparedUpdate.getValue() == null) {
            return;
        }

        var downloadFile = preparedUpdate.getValue().getFile();
        if (!Files.exists(downloadFile)) {
            return;
        }

        // Check if prepared update is still the latest.
        // We only do that here to minimize the sent requests by only executing when it's really necessary
        var available = XPipeDistributionType.get()
                .getUpdateHandler()
                .refreshUpdateCheckSilent(
                        false, preparedUpdate.getValue().isSecurityOnly());
        if (preparedUpdate.getValue() == null) {
            return;
        }

        if (available != null
                && !available.getVersion().equals(preparedUpdate.getValue().getVersion())) {
            preparedUpdate.setValue(null);
            return;
        }

        event("Executing update ...");
        executeUpdate();
    }

    public void executeUpdate() {
        throw new UnsupportedOperationException();
    }

    public final AvailableRelease refreshUpdateCheck(boolean first, boolean securityOnly) throws Exception {
        if (busy.getValue()) {
            return lastUpdateCheckResult.getValue();
        }

        try (var ignored = new BooleanScope(busy).start()) {
            return refreshUpdateCheckImpl(first, securityOnly);
        }
    }

    public abstract AvailableRelease refreshUpdateCheckImpl(boolean first, boolean securityOnly) throws Exception;

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
        String sourceDist;
        String version;
        String releaseUrl;
        String downloadUrl;
        AppInstaller.InstallerAssetType assetType;
        Instant checkTime;
        boolean isUpdate;
        boolean securityOnly;
    }

    @Value
    @Builder
    @Jacksonized
    public static class PreparedUpdate {
        String sourceVersion;
        String sourceDist;
        String version;
        String releaseUrl;
        Path file;
        String body;
        AppInstaller.InstallerAssetType assetType;
        boolean securityOnly;
    }
}
