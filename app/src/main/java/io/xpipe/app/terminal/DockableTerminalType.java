package io.xpipe.app.terminal;

public interface DockableTerminalType {

    public default int getProcessHierarchyOffset() {
        return 0;
    }
}
