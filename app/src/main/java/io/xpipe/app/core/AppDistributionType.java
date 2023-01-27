package io.xpipe.app.core;

import io.xpipe.core.util.XPipeInstallation;
import io.xpipe.extension.event.TrackEvent;

public interface AppDistributionType {

    AppDistributionType DEVELOPMENT = new AppDistributionType() {

        @Override
        public boolean supportsUpdate() {
            return true;
        }

        @Override
        public void performUpdateAction() {
            TrackEvent.info("Development mode update executed");
        }

        @Override
        public String getName() {
            return "development";
        }
    };
    AppDistributionType PORTABLE = new AppDistributionType() {

        @Override
        public boolean supportsUpdate() {
            return false;
        }

        @Override
        public void performUpdateAction() {}

        @Override
        public String getName() {
            return "portable";
        }
    };
    AppDistributionType INSTALLATION = new AppDistributionType() {

        @Override
        public boolean supportsUpdate() {
            return true;
        }

        @Override
        public void performUpdateAction() {
            TrackEvent.info("Update action called");
        }

        @Override
        public String getName() {
            return "install";
        }
    };

    static AppDistributionType get() {
        if (!AppProperties.get().isImage()) {
            return DEVELOPMENT;
        }

        if (XPipeInstallation.isInstallationDistribution()) {
            return INSTALLATION;
        } else {
            return PORTABLE;
        }
    }

    boolean supportsUpdate();

    void performUpdateAction();

    String getName();
}
