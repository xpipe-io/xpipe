package io.xpipe.app.hub.action;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import lombok.SneakyThrows;

import java.util.List;

public interface HubMenuLeafProvider<T extends DataStore> extends HubMenuItemProvider<T> {

    default boolean isDefault(DataStoreEntryRef<T> o) {
        return false;
    }

    default void execute(DataStoreEntryRef<T> ref) {
        createAction(ref).executeAsync();
    }

    @SneakyThrows
    default AbstractAction createAction(DataStoreEntryRef<T> ref) {
        var c = getActionClass().orElseThrow();
        var bm = c.getDeclaredMethod("builder");
        bm.setAccessible(true);
        var b = bm.invoke(null);

        if (StoreAction.class.isAssignableFrom(c)) {
            var refMethod = b.getClass().getMethod("ref", DataStoreEntryRef.class);
            refMethod.setAccessible(true);
            refMethod.invoke(b, ref);
        }

        var m = b.getClass().getDeclaredMethod("build");
        m.setAccessible(true);
        var defValue = c.cast(m.invoke(b));
        return defValue;
    }

    default boolean requiresValidStore() {
        return true;
    }
}
