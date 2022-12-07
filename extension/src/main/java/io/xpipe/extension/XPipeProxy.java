package io.xpipe.extension;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.XPipeInstallation;
import io.xpipe.extension.util.XPipeDaemon;

import java.io.IOException;

public class XPipeProxy {

    public static void checkSupport(ShellStore store) throws Exception {
        if (store == null || ShellStore.isLocal(store)) {
            return;
        }

        var version = XPipeDaemon.getInstance().getVersion();
        try (ShellProcessControl s = store.create().start()) {
            var defaultInstallationExecutable = FileNames.join(
                    XPipeInstallation.getDefaultInstallationBasePath(s),
                    XPipeInstallation.getDaemonExecutablePath(s.getOsType()));
            if (!s.executeBooleanSimpleCommand(
                    s.getShellType().createFileExistsCommand(defaultInstallationExecutable))) {
                throw new IOException(I18n.get("noInstallationFound"));
            }

            var installationVersion = XPipeInstallation.queryInstallationVersion(s, defaultInstallationExecutable);
            if (!version.equals(installationVersion)) {
                throw new IOException(I18n.get("versionMismatch", version, installationVersion));
            }
        }
    }
}
