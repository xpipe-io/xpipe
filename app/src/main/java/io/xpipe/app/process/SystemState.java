package io.xpipe.app.process;

import io.xpipe.core.OsType;

public interface SystemState {

    OsType.Any getOsType();

    String getOsName();

    ShellDialect getShellDialect();

    ShellTtyState getTtyState();
}
