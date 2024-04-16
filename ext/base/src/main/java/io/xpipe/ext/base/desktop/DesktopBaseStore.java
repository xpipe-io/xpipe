package io.xpipe.ext.base.desktop;

import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.store.DataStore;

public interface DesktopBaseStore extends DataStore {

    boolean supportsDesktopAccess();

    void runDesktopScript(String name, ShellDialect dialect, String script) throws Exception;

    void runDesktopTerminal(String name, ExternalTerminalType terminalType, ShellDialect dialect, String script) throws Exception;

    ShellDialect getUsedDialect();

    OsType getUsedOsType();
}
