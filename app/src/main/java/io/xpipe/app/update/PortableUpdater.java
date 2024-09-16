package io.xpipe.app.update;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.util.Hyperlinks;

import javafx.scene.layout.Region;

import org.kohsuke.github.GHRelease;

import java.time.Instant;

public class PortableUpdater extends UpdateHandler {

    public PortableUpdater(boolean thread) {
        super(thread);
    }

    @Override
    public Region createInterface() {
        return new ButtonComp(AppI18n.observable("checkOutUpdate"), () -> {
                    Hyperlinks.open(XPipeDistributionType.get()
                            .getUpdateHandler()
                            .getPreparedUpdate()
                            .getValue()
                            .getReleaseUrl());
                })
                .createRegion();
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
        lastUpdateCheckResult.setValue(new AvailableRelease(
                AppProperties.get().getVersion(),
                XPipeDistributionType.get().getId(),
                rel.get().getTagName(),
                rel.get().getHtmlUrl().toString(),
                null,
                null,
                Instant.now(),
                isUpdate));
        return lastUpdateCheckResult.getValue();
    }
}
