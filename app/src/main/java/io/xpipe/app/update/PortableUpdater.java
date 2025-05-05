package io.xpipe.app.update;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.util.Hyperlinks;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PortableUpdater extends UpdateHandler {

    public PortableUpdater(boolean thread) {
        super(thread);
    }

    @Override
    public boolean supportsDirectInstallation() {
        return false;
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
                true));
        return list;
    }

    public synchronized AvailableRelease refreshUpdateCheckImpl(boolean first, boolean securityOnly) throws Exception {
        var rel = AppDownloads.queryLatestRelease(first, securityOnly);
        event("Determined latest suitable release " + rel.getTag());
        var isUpdate = isUpdate(rel.getTag());
        lastUpdateCheckResult.setValue(new AvailableRelease(
                AppProperties.get().getVersion(),
                AppDistributionType.get().getId(),
                rel.getTag(),
                rel.getBrowserUrl(),
                null,
                null,
                Instant.now(),
                isUpdate,
                securityOnly));
        return lastUpdateCheckResult.getValue();
    }
}
