package io.xpipe.app.update;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.Hyperlinks;

import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GitHubUpdater extends UpdateHandler {

    public GitHubUpdater(boolean startBackgroundThread) {
        super(startBackgroundThread);
    }

    @Override
    public boolean supportsDirectInstallation() {
        return true;
    }

    @Override
    public List<ModalButton> createActions() {
        var list = new ArrayList<ModalButton>();
        list.add(new ModalButton("ignore", null, true, false));
        list.add(new ModalButton(
                "checkOutUpdate",
                () -> {
                    if (getPreparedUpdate().getValue() == null) {
                        return;
                    }

                    Hyperlinks.open(getPreparedUpdate().getValue().getReleaseUrl());
                },
                false,
                false));
        list.add(new ModalButton(
                "install",
                () -> {
                    executeUpdateAndClose();
                },
                true,
                true));
        return list;
    }

    public void prepareUpdateImpl() throws Exception {
        var downloadFile =
                AppDownloads.downloadInstaller(lastUpdateCheckResult.getValue().getVersion());
        var changelogString =
                AppDownloads.downloadChangelog(lastUpdateCheckResult.getValue().getVersion());
        var rel = new PreparedUpdate(
                AppProperties.get().getVersion(),
                AppDistributionType.get().getId(),
                lastUpdateCheckResult.getValue().getVersion(),
                lastUpdateCheckResult.getValue().getReleaseUrl(),
                downloadFile,
                changelogString,
                lastUpdateCheckResult.getValue().getAssetType(),
                lastUpdateCheckResult.getValue().isSecurityOnly());
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
            ErrorEventFactory.fromThrowable(t).handle();
            preparedUpdate.setValue(null);
        }
    }

    public synchronized AvailableRelease refreshUpdateCheckImpl(boolean first, boolean securityOnly) throws Exception {
        var rel = AppDownloads.queryLatestVersion(first, securityOnly);
        event("Determined latest suitable release " + rel.getTag());
        var isUpdate = isUpdate(rel.getTag());
        var assetType = AppInstaller.getSuitablePlatformAsset();
        event("Selected asset " + rel.getFile());
        lastUpdateCheckResult.setValue(new AvailableRelease(
                AppProperties.get().getVersion(),
                AppDistributionType.get().getId(),
                rel.getTag(),
                rel.getBrowserUrl(),
                rel.getUrl(),
                assetType,
                Instant.now(),
                isUpdate,
                securityOnly));
        return lastUpdateCheckResult.getValue();
    }
}
