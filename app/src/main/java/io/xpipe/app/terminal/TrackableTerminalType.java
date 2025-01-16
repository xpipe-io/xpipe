package io.xpipe.app.terminal;

public interface TrackableTerminalType {

    default int getProcessHierarchyOffset() {
        return 0;
    }
}
