package io.xpipe.app.webtop;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.update.AppDistributionType;

public class WebtopPreconfiguredDialog {

    public static void showIfNeeded() {
        if (AppDistributionType.get() != AppDistributionType.WEBTOP) {
            return;
        }

        if (!AppProperties.get().isInitialLaunch()) {
            return;
        }

        // TODO: This is disabled for now
        if (true) {
            return;
        }

        AppDialog.information("webtopPreconfiguredDialog");
    }
}
