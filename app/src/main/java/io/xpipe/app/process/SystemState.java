package io.xpipe.app.process;

import io.xpipe.core.OsType;

public interface SystemState {

    OsType getOsType();

    String getOsName();

    ShellDialect getShellDialect();

    ShellTtyState getTtyState();
}
