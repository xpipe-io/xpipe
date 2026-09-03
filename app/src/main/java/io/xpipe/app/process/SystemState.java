package io.xpipe.app.process;

import io.xpipe.app.util.OsType;

public interface SystemState {

    OsType.Any getOsType();

    String getOsName();

    ShellDialect getShellDialect();

    ShellTtyState getTtyState();
}
