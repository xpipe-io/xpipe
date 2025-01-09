package io.xpipe.ext.base.desktop;

import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.store.DataStore;

public interface DesktopBaseStore extends DataStore {

    boolean supportsDesktopAccess();

    void runDesktopApplication(String name, DesktopApplicationStore applicationStore) throws Exception;

    ShellDialect getUsedDesktopDialect();

}
