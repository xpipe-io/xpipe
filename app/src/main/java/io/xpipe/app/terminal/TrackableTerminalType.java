package io.xpipe.app.terminal;

public interface TrackableTerminalType {

    public default int getProcessHierarchyOffset() {
        return 0;
    }
}
