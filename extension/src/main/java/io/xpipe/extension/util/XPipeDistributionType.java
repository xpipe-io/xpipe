package io.xpipe.extension.util;

import io.xpipe.core.process.OsType;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.XPipeInstallation;
import io.xpipe.extension.event.TrackEvent;

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
        public boolean supportsURLs() {
            // Enabled for testing
            return true;
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
        public boolean supportsURLs() {
            return OsType.getLocal().equals(OsType.MACOS);
        }

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
        public boolean supportsURLs() {
            return true;
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

    boolean supportsURLs();

    String getName();
}
