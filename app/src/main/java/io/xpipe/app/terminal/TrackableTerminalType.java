package io.xpipe.app.terminal;

public interface TrackableTerminalType extends ExternalTerminalType {

    default int getProcessHierarchyOffset() {
        return 0;
    }
}
