package io.xpipe.ext.base.desktop;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.process.ShellDialect;

public interface DesktopBaseStore extends DataStore {

    boolean supportsDesktopAccess();

    void runDesktopApplication(String name, DesktopApplicationStore applicationStore) throws Exception;

    ShellDialect getUsedDesktopDialect();
}
