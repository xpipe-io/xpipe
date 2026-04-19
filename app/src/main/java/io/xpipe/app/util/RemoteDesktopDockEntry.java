package io.xpipe.app.util;

import io.xpipe.app.storage.DataStoreColor;
import lombok.Value;

@Value
public class RemoteDesktopDockEntry {

    String name;
    String icon;
    DataStoreColor color;

    ControllableWindowProcess process;
    RemoteDesktopDockContentEntry internal;

    public boolean isInternal() {
        return internal != null;
    }

    public boolean isExternal() {
        return process != null;
    }
}
