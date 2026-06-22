package io.xpipe.app.webtop;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.ThreadHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class WebtopPreconfiguredDialog {

    public static void showIfNeeded() {
        if (AppDistributionType.get() != AppDistributionType.WEBTOP) {
            return;
        }

        if (!AppProperties.get().isInitialLaunch()) {
            return;
        }

        if ("true".equals(System.getenv("XPIPE_WIZARD_PRECONFIGURED"))) {
            return;
        }

        AppDialog.information("webtopPreconfiguredDialog");
    }
}
