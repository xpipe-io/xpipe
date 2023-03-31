package io.xpipe.app.util;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.XPipeInstallation;

public interface XPipeDistributionType {

    XPipeDistributionType DEVELOPMENT = new XPipeDistributionType() {

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
    XPipeDistributionType PORTABLE = new XPipeDistributionType() {

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
    XPipeDistributionType INSTALLATION = new XPipeDistributionType() {

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

    static XPipeDistributionType get() {
        if (!ModuleHelper.isImage()) {
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
