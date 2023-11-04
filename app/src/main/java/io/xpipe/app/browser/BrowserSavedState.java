package io.xpipe.app.browser;

import io.xpipe.app.core.AppCache;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Jacksonized
@Builder
@Getter
public class BrowserSavedState {

    @NonNull List<Entry> lastSystems;

    static BrowserSavedState load() {
        return AppCache.get("browser-state", BrowserSavedState.class, () -> {
            return null;
        });
    }

    public void save() {
        AppCache.update("browser-state", this);
    }

    @Value
    @Jacksonized
    @Builder
    public static class Entry {

        UUID uuid;
        String path;
    }
}
