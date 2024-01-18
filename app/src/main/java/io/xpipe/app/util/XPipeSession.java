package io.xpipe.app.util;

import io.xpipe.app.core.AppCache;
import lombok.Value;

import java.util.UUID;

@Value
public class XPipeSession {

    boolean isNewBuildSession;

    /**
     * Unique identifier that resets on every XPipe restart.
     */
    UUID sessionId;

    /**
     * Unique identifier that resets on every XPipe update.
     */
    UUID buildSessionId;


    private static XPipeSession INSTANCE;

    public static void init(UUID buildSessionId) {
        if (INSTANCE != null) {
            return;
        }

        var s = AppCache.get("lastBuildId", String.class, () -> null);
        var isBuildChanged = !buildSessionId.toString().equals(s);
        AppCache.update("lastBuildId", buildSessionId.toString());
        INSTANCE = new XPipeSession(isBuildChanged, UUID.randomUUID(), buildSessionId);
    }

    public static XPipeSession get() {
        return INSTANCE;
    }
}
