package io.xpipe.app.update;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import javafx.scene.layout.Region;
import org.kohsuke.github.GHRelease;

import java.nio.file.Files;
import java.time.Instant;

public class GitHubUpdater extends UpdateHandler {

    public GitHubUpdater(boolean startBackgroundThread) {
        super(startBackgroundThread);
    }

    @Override
    public Region createInterface() {
        return null;
    }

    public void prepareUpdateImpl() {
        var downloadFile = AppDownloads.downloadInstaller(
                lastUpdateCheckResult.getValue().getAssetType(),
                lastUpdateCheckResult.getValue().getVersion(),
                false);
        if (downloadFile.isEmpty()) {
            return;
        }

        var changelogString =
                AppDownloads.downloadChangelog(lastUpdateCheckResult.getValue().getVersion(), false);
        var changelog = changelogString.orElse(null);
        var rel = new PreparedUpdate(
                AppProperties.get().getVersion(),
                XPipeDistributionType.get().getId(),
                lastUpdateCheckResult.getValue().getVersion(),
                lastUpdateCheckResult.getValue().getReleaseUrl(),
                downloadFile.get(),
                changelog,
                lastUpdateCheckResult.getValue().getAssetType());
        preparedUpdate.setValue(rel);
    }

    public void executeUpdate() {
        var p = preparedUpdate.getValue();
        var downloadFile = p.getFile();
        if (!Files.exists(downloadFile)) {
            event("Prepared update file does not exist");
            return;
        }

        try {
            var performedUpdate = new PerformedUpdate(p.getVersion(), p.getBody(), p.getVersion());
            AppCache.update("performedUpdate", performedUpdate);

            var a = p.getAssetType();
            a.installLocal(downloadFile);
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).handle();
            preparedUpdate.setValue(null);
        }
    }

    public synchronized AvailableRelease refreshUpdateCheckImpl() throws Exception {
        var rel = AppDownloads.getLatestSuitableRelease();
        event("Determined latest suitable release "
                + rel.map(GHRelease::getName).orElse(null));

        if (rel.isEmpty()) {
            lastUpdateCheckResult.setValue(null);
            return null;
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
                XPipeDistributionType.get().getId(),
                rel.get().getTagName(),
                rel.get().getHtmlUrl().toString(),
                ghAsset.get().getBrowserDownloadUrl(),
                assetType,
                Instant.now(),
                isUpdate));
        return lastUpdateCheckResult.getValue();
    }
}
