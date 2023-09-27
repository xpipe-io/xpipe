package io.xpipe.app.core;

import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.UUID;

@Value
public class AppState {

    private static AppState INSTANCE;

    UUID userId;
    boolean initialLaunch;

    @NonFinal
            @Setter
    String userName;
    @NonFinal
    @Setter
    String userEmail;

    public AppState() {
        UUID id = AppCache.get("userId", UUID.class, null);
        if (id == null) {
            initialLaunch = AppCache.getIfPresent("lastBuild", String.class).isEmpty();
            userId = UUID.randomUUID();
            AppCache.update("userId", userId);
        } else {
            userId = id;
            initialLaunch = false;
        }
    }

    public static void init() {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = new AppState();
    }

    public static AppState get() {
        return INSTANCE;
    }
}
