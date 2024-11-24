package io.xpipe.core.process;

public interface SystemState {

    OsType getOsType();

    String getOsName();

    ShellDialect getShellDialect();

    ShellTtyState getTtyState();
}
