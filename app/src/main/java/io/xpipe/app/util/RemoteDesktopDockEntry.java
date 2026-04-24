package io.xpipe.app.util;

import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;
import lombok.Value;

@Value
public class RemoteDesktopDockEntry {

    String name;
    String icon;
    DataStoreColor color;
    DataStoreEntry entry;

    ControllableWindowProcess process;
    RemoteDesktopDockContentEntry internal;

    boolean lockedSize;
    Integer initialWidth;
    Integer initialHeight;

    public boolean requiresRestart(int w, int h) {
        return isExternal() &&
                isLockedSize() && (w != getInitialWidth() || h != getInitialHeight());
    }

    public boolean isInternal() {
        return internal != null;
    }

    public boolean isExternal() {
        return process != null;
    }
}
