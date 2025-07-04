package io.xpipe.app.ext;

import io.xpipe.app.hub.comp.StoreEntryComp;
import io.xpipe.app.hub.comp.StoreSection;
import io.xpipe.app.hub.comp.StoreToggleComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.property.SimpleBooleanProperty;

public interface EnabledParentStoreProvider extends DataStoreProvider {

    @Override
    default StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        if (sec.getWrapper().getValidity().getValue() == DataStoreEntry.Validity.LOAD_FAILED) {
            return StoreEntryComp.create(sec, null, preferLarge);
        }

        EnabledStoreState initialState = sec.getWrapper().getEntry().getStorePersistentState();
        var enabled = new SimpleBooleanProperty(initialState.isEnabled());
        sec.getWrapper().getPersistentState().subscribe((newValue) -> {
            EnabledStoreState s = sec.getWrapper().getEntry().getStorePersistentState();
            enabled.set(s.isEnabled());
        });

        var toggle = StoreToggleComp.<StatefulDataStore<EnabledStoreState>>enableToggle(
                null, sec, enabled, (s, aBoolean) -> {
                    var state = s.getState().toBuilder().enabled(aBoolean).build();
                    s.setState(state);

                    var children =
                            DataStorage.get().getStoreChildren(sec.getWrapper().getEntry());
                    ThreadHelper.runFailableAsync(() -> {
                        for (DataStoreEntry child : children) {
                            if (child.getStorePersistentState() instanceof EnabledStoreState enabledStoreState) {
                                child.setStorePersistentState(enabledStoreState.toBuilder()
                                        .enabled(aBoolean)
                                        .build());
                            }
                        }
                    });
                });

        var e = sec.getWrapper().getEntry();
        var parent = DataStorage.get().getDefaultDisplayParent(e);
        if (parent.isPresent()) {
            var parentWrapper = StoreViewState.get().getEntryWrapper(parent.get());
            // Disable selection if parent is already made enabled
            toggle.setCustomVisibility(BindingsHelper.map(parentWrapper.getPersistentState(), o -> {
                EnabledStoreState state = (EnabledStoreState) o;
                return !state.isEnabled();
            }));
        }

        return StoreEntryComp.create(sec, toggle, preferLarge);
    }
}
