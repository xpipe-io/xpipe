package io.xpipe.app.hub.section;

import io.xpipe.app.hub.entry.StoreEntryWrapper;

public interface StoreSectionSelector {

    boolean excludeNonShown();

    boolean matches(StoreEntryWrapper wrapper);
}
