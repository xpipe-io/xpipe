package io.xpipe.app.util;

import io.xpipe.app.comp.BaseRegionBuilder;
import lombok.Getter;

@Getter
public abstract class RemoteDesktopDockContentEntry {

    private final Runnable onKill;

    protected RemoteDesktopDockContentEntry(Runnable onKill) {this.onKill = onKill;}

    public abstract BaseRegionBuilder<?, ?> comp();

    public abstract void init() throws Exception;

    public abstract void close();
}
