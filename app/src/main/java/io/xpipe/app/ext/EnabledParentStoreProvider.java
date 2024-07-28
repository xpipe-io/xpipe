package io.xpipe.app.ext;

import io.xpipe.app.comp.base.StoreToggleComp;
import io.xpipe.app.comp.store.StoreEntryComp;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.EnabledStoreState;
import io.xpipe.core.store.StatefulDataStore;

public interface EnabledParentStoreProvider extends DataStoreProvider {

    @Override
    default StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        if (sec.getWrapper().getValidity().getValue() == DataStoreEntry.Validity.LOAD_FAILED) {
            return StoreEntryComp.create(sec, null, preferLarge);
        }

        var enabled = StoreToggleComp.<StatefulDataStore<EnabledStoreState>>enableToggle(
                null, sec, s -> s.getState().isEnabled(), (s, aBoolean) -> {
                    var state = s.getState().toBuilder().enabled(aBoolean).build();
                    s.setState(state);
                });

        var e = sec.getWrapper().getEntry();
        var parent = DataStorage.get().getDefaultDisplayParent(e);
        if (parent.isPresent()) {
            var parentWrapper = StoreViewState.get().getEntryWrapper(parent.get());
            // Disable selection if parent is already made enabled
            enabled.setCustomVisibility(BindingsHelper.map(parentWrapper.getPersistentState(), o -> {
                EnabledStoreState state = (EnabledStoreState) o;
                return !state.isEnabled();
            }));
        }

        return StoreEntryComp.create(sec, enabled, preferLarge);
    }
}
