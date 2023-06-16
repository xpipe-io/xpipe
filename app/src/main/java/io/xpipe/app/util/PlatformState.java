package io.xpipe.app.util;

import lombok.Getter;
import lombok.Setter;

public enum PlatformState {
    NOT_INITIALIZED,
    RUNNING,
    EXITED;

    @Getter
    @Setter
    private static PlatformState current = PlatformState.NOT_INITIALIZED;
}
