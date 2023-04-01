package io.xpipe.app.util;

import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.XPipeInstallation;

public interface XPipeDistributionType {

    XPipeDistributionType DEVELOPMENT = new XPipeDistributionType() {

        @Override
        public boolean checkForUpdateOnStartup() {
            return false;
        }

        @Override
        public boolean supportsUpdate() {
            return true;
        }

        @Override
        public String getName() {
            return "development";
        }
    };
    XPipeDistributionType PORTABLE = new XPipeDistributionType() {

        @Override
        public boolean checkForUpdateOnStartup() {
            return false;
        }

        @Override
        public boolean supportsUpdate() {
            return false;
        }

        @Override
        public String getName() {
            return "portable";
        }
    };
    XPipeDistributionType INSTALLATION = new XPipeDistributionType() {

        @Override
        public boolean checkForUpdateOnStartup() {
            return true;
        }

        @Override
        public boolean supportsUpdate() {
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

    boolean checkForUpdateOnStartup();

    boolean supportsUpdate();

    String getName();
}
