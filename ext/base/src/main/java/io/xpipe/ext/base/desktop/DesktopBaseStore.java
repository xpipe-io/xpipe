package io.xpipe.ext.base.desktop;

import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FilePath;

public interface DesktopBaseStore extends DataStore {

    boolean supportsDesktopAccess();

    void runDesktopApplication(String name, DesktopApplicationStore applicationStore) throws Exception;

    void runDesktopScript(String name, String script) throws Exception;

    FilePath createScript(ShellDialect dialect, String content) throws Exception;

    ShellDialect getUsedDialect();

    OsType getUsedOsType();
}
